package com.example.adkassistant.ui.main

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.adkassistant.agent.AssistantAgent
import com.google.adk.kt.runners.InMemoryRunner
import com.google.adk.kt.sessions.InMemorySessionService
import com.google.adk.kt.types.Content
import com.google.adk.kt.types.Part
import com.google.adk.kt.types.Role
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

enum class Author { USER, AGENT }

data class ChatMessage(val author: Author, val text: String)

data class ChatUiState(
    val messages: List<ChatMessage> = emptyList(),
    val isThinking: Boolean = false,
)

/**
 * Drives a single conversation with [AssistantAgent].
 *
 * The runner owns session state across turns; we keep one [InMemoryRunner] and a
 * stable session id for the lifetime of the screen so the agent remembers the
 * conversation.
 */
class MainScreenViewModel : ViewModel() {

    private val runner = InMemoryRunner(
        agent = AssistantAgent.rootAgent,
        sessionService = InMemorySessionService(),
    )

    private val userId = "local-user"
    private val sessionId = "local-session"

    private val _uiState = MutableStateFlow(ChatUiState())
    val uiState: StateFlow<ChatUiState> = _uiState.asStateFlow()

    fun send(prompt: String) {
        val text = prompt.trim()
        if (text.isEmpty() || _uiState.value.isThinking) return

        _uiState.update {
            it.copy(
                messages = it.messages + ChatMessage(Author.USER, text),
                isThinking = true,
            )
        }

        viewModelScope.launch {
            val reply = runCatching { ask(text) }
                .getOrElse { e -> "Something went wrong: ${e.message}" }
            _uiState.update {
                it.copy(
                    messages = it.messages + ChatMessage(Author.AGENT, reply),
                    isThinking = false,
                )
            }
        }
    }

    /** Runs one turn and returns the agent's final text. */
    private suspend fun ask(text: String): String = withContext(Dispatchers.IO) {
        val message = Content(
            role = Role.USER,
            parts = listOf(Part(text = text)),
        )

        val builder = StringBuilder()
        runner.runAsync(
            userId = userId,
            sessionId = sessionId,
            newMessage = message,
        ).collect { event ->
            event.content?.parts
                ?.mapNotNull { it.text }
                ?.forEach { builder.append(it) }
        }
        builder.toString().ifBlank { "(no response)" }
    }
}
