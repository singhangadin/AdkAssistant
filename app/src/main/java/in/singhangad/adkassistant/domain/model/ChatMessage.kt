package `in`.singhangad.adkassistant.domain.model

/** Who authored a message in the conversation. */
enum class Author { USER, AGENT }

/** A single message in a conversation with the assistant. */
data class ChatMessage(val author: Author, val text: String)
