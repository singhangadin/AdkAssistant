package `in`.singhangad.adkassistant.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import `in`.singhangad.adkassistant.data.local.entity.MessageEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface MessageDao {

    @Query("SELECT * FROM messages WHERE conversationId = :conversationId ORDER BY createdAt ASC, id ASC")
    fun observeForConversation(conversationId: String): Flow<List<MessageEntity>>

    @Query("SELECT * FROM messages WHERE conversationId = :conversationId ORDER BY createdAt ASC, id ASC")
    suspend fun getForConversation(conversationId: String): List<MessageEntity>

    @Insert
    suspend fun insert(message: MessageEntity): Long
}
