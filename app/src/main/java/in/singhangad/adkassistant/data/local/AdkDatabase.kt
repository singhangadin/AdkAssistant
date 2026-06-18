package `in`.singhangad.adkassistant.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import `in`.singhangad.adkassistant.data.local.dao.ConversationDao
import `in`.singhangad.adkassistant.data.local.dao.MessageDao
import `in`.singhangad.adkassistant.data.local.entity.ConversationEntity
import `in`.singhangad.adkassistant.data.local.entity.MessageEntity

@Database(
    entities = [ConversationEntity::class, MessageEntity::class],
    version = 1,
    exportSchema = false,
)
abstract class AdkDatabase : RoomDatabase() {
    abstract fun conversationDao(): ConversationDao
    abstract fun messageDao(): MessageDao
}
