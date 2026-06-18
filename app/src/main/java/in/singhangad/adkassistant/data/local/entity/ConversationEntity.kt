package `in`.singhangad.adkassistant.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/** A conversation thread. Its [id] doubles as the ADK session id. */
@Entity(tableName = "conversations")
data class ConversationEntity(
    @PrimaryKey val id: String,
    val title: String,
    val createdAt: Long,
    val updatedAt: Long,
)
