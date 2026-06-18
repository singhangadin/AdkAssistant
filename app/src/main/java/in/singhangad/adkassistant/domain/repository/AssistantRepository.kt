package `in`.singhangad.adkassistant.domain.repository

import `in`.singhangad.adkassistant.domain.model.AgentStatus
import `in`.singhangad.adkassistant.domain.model.ChatMessage

/**
 * Boundary between the domain and the agent runtime.
 *
 * The domain knows nothing about ADK, Gemini, or sessions — only that it can
 * send a prompt within a conversation and get a reply back.
 */
interface AssistantRepository {
    /**
     * Sends [prompt] within conversation [conversationId] and returns the reply.
     *
     * [history] is the prior transcript (oldest first, excluding [prompt]); on the
     * first send to a conversation in this process it is replayed into the agent's
     * session so the model remembers earlier turns after an app restart. [onStatus]
     * reports progress (thinking / retrying) so the UI can show live feedback.
     */
    suspend fun send(
        conversationId: String,
        prompt: String,
        history: List<ChatMessage>,
        onStatus: (AgentStatus) -> Unit = {},
    ): String
}
