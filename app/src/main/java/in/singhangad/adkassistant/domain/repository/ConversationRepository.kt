package `in`.singhangad.adkassistant.domain.repository

import `in`.singhangad.adkassistant.domain.model.ChatMessage
import `in`.singhangad.adkassistant.domain.model.Conversation
import kotlinx.coroutines.flow.Flow

/** Stores conversation threads and their messages (the persisted transcript). */
interface ConversationRepository {

    fun observeConversations(): Flow<List<Conversation>>

    fun observeMessages(conversationId: String): Flow<List<ChatMessage>>

    /** Messages so far, oldest first — used to replay context into a session. */
    suspend fun getMessages(conversationId: String): List<ChatMessage>

    /** Creates an empty conversation and returns its id. */
    suspend fun createConversation(): String

    suspend fun addMessage(conversationId: String, message: ChatMessage)

    suspend fun renameConversation(conversationId: String, title: String)

    suspend fun deleteConversation(conversationId: String)
}
