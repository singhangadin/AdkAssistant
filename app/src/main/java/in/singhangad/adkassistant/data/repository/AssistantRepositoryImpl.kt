package `in`.singhangad.adkassistant.data.repository

import com.google.adk.kt.events.Event
import com.google.adk.kt.runners.InMemoryRunner
import com.google.adk.kt.sessions.GetSessionConfig
import com.google.adk.kt.sessions.InMemorySessionService
import com.google.adk.kt.sessions.Session
import com.google.adk.kt.sessions.SessionKey
import com.google.adk.kt.types.Content
import com.google.adk.kt.types.Part
import com.google.adk.kt.types.Role
import `in`.singhangad.adkassistant.data.agent.AssistantAgentFactory
import `in`.singhangad.adkassistant.domain.model.AgentStatus
import `in`.singhangad.adkassistant.domain.model.Author
import `in`.singhangad.adkassistant.domain.model.ChatMessage
import `in`.singhangad.adkassistant.domain.repository.AssistantRepository
import `in`.singhangad.adkassistant.domain.repository.ModelRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.flow.transformWhile
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Talks to the ADK [InMemoryRunner], one session per conversation.
 *
 * The runner is built for the user's selected model and rebuilt when the model
 * changes (ADK fixes the model at construction). Because the runner's
 * [InMemorySessionService] is in-memory — and a new one starts empty after a
 * model switch or process restart — the first send to a conversation replays its
 * stored transcript into the session, so the model still remembers earlier turns.
 */
@Singleton
class AssistantRepositoryImpl @Inject constructor(
    private val agentFactory: AssistantAgentFactory,
    private val modelRepository: ModelRepository,
    private val ioDispatcher: CoroutineDispatcher,
) : AssistantRepository {

    private var currentModel: String? = null
    private var runner: InMemoryRunner? = null
    private val hydrated = mutableSetOf<String>()
    private val lock = Mutex()

    override suspend fun send(
        conversationId: String,
        prompt: String,
        history: List<ChatMessage>,
        onStatus: (AgentStatus) -> Unit,
    ): String = withContext(ioDispatcher) {
        val activeRunner = runnerFor(modelRepository.selectedModel())
        ensureHydrated(activeRunner, conversationId, history)
        onStatus(AgentStatus.Thinking)

        // Gemini intermittently returns 503 "high demand". Google's guidance for
        // that is exponential backoff, so retry transient failures a few times
        // before surfacing an error. Non-transient errors fail fast.
        var attempt = 0
        while (true) {
            try {
                return@withContext runTurn(activeRunner, conversationId, prompt)
            } catch (e: Exception) {
                if (!e.isTransient() || attempt >= MAX_RETRIES) {
                    throw if (e.isTransient()) {
                        IllegalStateException("The model is temporarily unavailable. Please try again.", e)
                    } else e
                }
                attempt++
                onStatus(AgentStatus.Retrying(attempt, MAX_RETRIES))
                delay(BASE_BACKOFF_MS shl (attempt - 1)) // 1s, 2s, 4s
            }
        }
        @Suppress("UNREACHABLE_CODE") error("unreachable")
    }

    /** Returns a runner for [model], rebuilding it (and resetting sessions) on change. */
    private suspend fun runnerFor(model: String): InMemoryRunner = lock.withLock {
        val existing = runner
        if (existing != null && currentModel == model) return existing
        InMemoryRunner(
            agent = agentFactory.create(model),
            sessionService = InMemorySessionService(),
        ).also {
            runner = it
            currentModel = model
            hydrated.clear() // fresh session service → conversations must re-hydrate
        }
    }

    /** Runs a single turn and returns the agent's final text. */
    private suspend fun runTurn(runner: InMemoryRunner, conversationId: String, prompt: String): String {
        val message = Content(role = Role.USER, parts = listOf(Part(text = prompt)))

        // Stop at the final response: the runner's event Flow can stay open after
        // the answer, so draining it with collect{} would hang the UI. withTimeout
        // is a backstop in case no final response ever arrives.
        val events = withTimeout(REPLY_TIMEOUT_MS) {
            runner.runAsync(
                userId = USER_ID,
                sessionId = conversationId,
                newMessage = message,
            ).transformWhile { event ->
                emit(event)
                !event.isFinalResponse
            }.toList()
        }

        events.firstNotNullOfOrNull { it.errorMessage }?.let { throw IllegalStateException(it) }

        return events.lastOrNull { it.isFinalResponse }
            ?.content?.parts
            ?.mapNotNull { it.text }
            ?.joinToString("")
            ?.ifBlank { "(no response)" }
            ?: "(no response)"
    }

    /**
     * Transient = worth retrying. Besides overload (503/UNAVAILABLE) and rate
     * limits (429), Gemini under load also emits spurious 404s ("no longer
     * available") for models that the list endpoint still serves — so treat those
     * as transient too rather than failing as if the model were gone.
     */
    private fun Throwable.isTransient(): Boolean {
        val text = (message ?: "").lowercase()
        return listOf(
            "503", "unavailable", "high demand", "overloaded", "try again",
            "429", "resource_exhausted",
            "404", "not found", "not_found", "no longer available",
        ).any { it in text }
    }

    /** Replays [history] into the session the first time we touch [conversationId]. */
    private suspend fun ensureHydrated(
        runner: InMemoryRunner,
        conversationId: String,
        history: List<ChatMessage>,
    ) = lock.withLock {
        if (conversationId in hydrated) return@withLock
        if (history.isNotEmpty()) {
            val key = SessionKey(runner.appName, USER_ID, conversationId)
            val session = getOrCreateSession(runner, key)
            if (session.events.isEmpty()) {
                history.forEach { runner.sessionService.appendEvent(session, it.toEvent()) }
            }
        }
        hydrated += conversationId
    }

    private suspend fun getOrCreateSession(runner: InMemoryRunner, key: SessionKey): Session =
        runCatching { runner.sessionService.getSession(key, GetSessionConfig()) }.getOrNull()
            ?: runner.sessionService.createSession(key, emptyMap())

    private fun ChatMessage.toEvent(): Event {
        val isUser = author == Author.USER
        return Event(
            author = if (isUser) "user" else AGENT_NAME,
            content = Content(
                role = if (isUser) Role.USER else Role.MODEL,
                parts = listOf(Part(text = text)),
            ),
        )
    }

    private companion object {
        const val USER_ID = "local-user"
        const val AGENT_NAME = "device_assistant"
        const val REPLY_TIMEOUT_MS = 60_000L
        const val MAX_RETRIES = 3
        const val BASE_BACKOFF_MS = 1_000L
    }
}
