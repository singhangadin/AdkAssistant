package `in`.singhangad.adkassistant.data.repository

import `in`.singhangad.adkassistant.data.local.dao.ConversationDao
import `in`.singhangad.adkassistant.data.local.dao.MessageDao
import `in`.singhangad.adkassistant.data.local.entity.ConversationEntity
import `in`.singhangad.adkassistant.data.local.entity.MessageEntity
import `in`.singhangad.adkassistant.domain.model.Author
import `in`.singhangad.adkassistant.domain.model.ChatMessage
import `in`.singhangad.adkassistant.domain.model.Conversation
import `in`.singhangad.adkassistant.domain.repository.ConversationRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.UUID
import javax.inject.Inject

class ConversationRepositoryImpl @Inject constructor(
    private val conversationDao: ConversationDao,
    private val messageDao: MessageDao,
) : ConversationRepository {

    override fun observeConversations(): Flow<List<Conversation>> =
        conversationDao.observeAll().map { list -> list.map { it.toDomain() } }

    override fun observeMessages(conversationId: String): Flow<List<ChatMessage>> =
        messageDao.observeForConversation(conversationId).map { list -> list.map { it.toDomain() } }

    override suspend fun getMessages(conversationId: String): List<ChatMessage> =
        messageDao.getForConversation(conversationId).map { it.toDomain() }

    override suspend fun createConversation(): String {
        val now = System.currentTimeMillis()
        val id = UUID.randomUUID().toString()
        conversationDao.upsert(
            ConversationEntity(id = id, title = DEFAULT_TITLE, createdAt = now, updatedAt = now)
        )
        return id
    }

    override suspend fun addMessage(conversationId: String, message: ChatMessage) {
        val now = System.currentTimeMillis()
        messageDao.insert(
            MessageEntity(
                conversationId = conversationId,
                author = message.author.name,
                text = message.text,
                createdAt = now,
            )
        )
        // Auto-title the thread from the first user message, ChatGPT-style.
        val conversation = conversationDao.getById(conversationId)
        if (message.author == Author.USER && conversation?.title == DEFAULT_TITLE) {
            conversationDao.updateTitle(conversationId, message.text.toTitle(), now)
        } else {
            conversationDao.touch(conversationId, now)
        }
    }

    override suspend fun renameConversation(conversationId: String, title: String) {
        val clean = title.trim().ifBlank { DEFAULT_TITLE }
        conversationDao.updateTitle(conversationId, clean, System.currentTimeMillis())
    }

    override suspend fun deleteConversation(conversationId: String) {
        conversationDao.delete(conversationId) // messages cascade
    }

    private fun ConversationEntity.toDomain() = Conversation(id, title, updatedAt)

    private fun MessageEntity.toDomain() =
        ChatMessage(author = Author.valueOf(author), text = text)

    private fun String.toTitle(): String {
        val firstLine = trim().lineSequence().firstOrNull()?.trim().orEmpty()
        return if (firstLine.length <= TITLE_MAX) firstLine
        else firstLine.take(TITLE_MAX).trimEnd() + "…"
    }

    private companion object {
        const val DEFAULT_TITLE = "New chat"
        const val TITLE_MAX = 40
    }
}
