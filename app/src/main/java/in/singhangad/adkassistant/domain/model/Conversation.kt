package `in`.singhangad.adkassistant.domain.model

/** A conversation thread shown in the history drawer. */
data class Conversation(
    val id: String,
    val title: String,
    val updatedAt: Long,
)
