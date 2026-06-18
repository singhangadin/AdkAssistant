package `in`.singhangad.adkassistant.domain.usecase

import `in`.singhangad.adkassistant.domain.model.AgentStatus
import `in`.singhangad.adkassistant.domain.model.Author
import `in`.singhangad.adkassistant.domain.model.ChatMessage
import `in`.singhangad.adkassistant.domain.repository.AssistantRepository
import `in`.singhangad.adkassistant.domain.repository.ConversationRepository
import javax.inject.Inject

/**
 * Sends one user message within a conversation and persists both sides.
 *
 * Messages are stored in [ConversationRepository] (the single source of truth the
 * UI observes); the prior transcript is handed to the agent so a resumed
 * conversation keeps its context.
 */
class SendMessageUseCase @Inject constructor(
    private val conversations: ConversationRepository,
    private val assistant: AssistantRepository,
) {
    suspend operator fun invoke(
        conversationId: String,
        prompt: String,
        onStatus: (AgentStatus) -> Unit = {},
    ) {
        val history = conversations.getMessages(conversationId) // before adding the new turn
        conversations.addMessage(conversationId, ChatMessage(Author.USER, prompt))
        val reply = assistant.send(conversationId, prompt, history, onStatus)
        conversations.addMessage(conversationId, ChatMessage(Author.AGENT, reply))
    }
}
