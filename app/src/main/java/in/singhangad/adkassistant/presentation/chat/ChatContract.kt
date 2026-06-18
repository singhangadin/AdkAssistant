package `in`.singhangad.adkassistant.presentation.chat

import `in`.singhangad.adkassistant.domain.model.ChatMessage
import `in`.singhangad.adkassistant.domain.model.Conversation

/** The single immutable state the chat screen renders (MVI "Model"). */
data class ChatState(
    val conversations: List<Conversation> = emptyList(),
    val currentConversationId: String? = null,
    val messages: List<ChatMessage> = emptyList(),
    val input: String = "",
    val isThinking: Boolean = false,
    val thinkingLabel: String = "Thinking…",
    val availableModels: List<String> = emptyList(),
    val selectedModel: String = "",
) {
    val currentTitle: String
        get() = conversations.firstOrNull { it.id == currentConversationId }?.title ?: "ADK Assistant"
}

/** User intents the chat screen can dispatch (MVI "Intent"). */
sealed interface ChatIntent {
    data class InputChanged(val text: String) : ChatIntent
    data object Send : ChatIntent
    data object NewChat : ChatIntent
    data class OpenConversation(val id: String) : ChatIntent
    data class RenameConversation(val id: String, val title: String) : ChatIntent
    data class DeleteConversation(val id: String) : ChatIntent
    data class SelectModel(val model: String) : ChatIntent
}

/** One-off side effects the screen consumes once (MVI "Effect"). */
sealed interface ChatEffect {
    data class ShowError(val message: String) : ChatEffect
    data object CloseDrawer : ChatEffect
}
