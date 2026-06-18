package `in`.singhangad.adkassistant.presentation.chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import `in`.singhangad.adkassistant.domain.model.AgentStatus
import `in`.singhangad.adkassistant.domain.repository.ConversationRepository
import `in`.singhangad.adkassistant.domain.repository.ModelRepository
import `in`.singhangad.adkassistant.domain.usecase.SendMessageUseCase
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * MVI ViewModel for the chat + history drawer.
 *
 * Messages and conversations come straight from [ConversationRepository] (Room is
 * the single source of truth); the ViewModel only reduces intents and tracks the
 * selected conversation.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class ChatViewModel @Inject constructor(
    private val conversations: ConversationRepository,
    private val models: ModelRepository,
    private val sendMessage: SendMessageUseCase,
) : ViewModel() {

    private val _state = MutableStateFlow(ChatState())
    val state: StateFlow<ChatState> = _state.asStateFlow()

    private val _effects = Channel<ChatEffect>(Channel.BUFFERED)
    val effects = _effects.receiveAsFlow()

    private val selectedId = MutableStateFlow<String?>(null)

    init {
        conversations.observeConversations()
            .onEach { list ->
                _state.update { it.copy(conversations = list) }
                if (selectedId.value == null) {
                    selectedId.value = list.firstOrNull()?.id ?: conversations.createConversation()
                }
            }
            .launchIn(viewModelScope)

        selectedId
            .onEach { id -> _state.update { it.copy(currentConversationId = id) } }
            .flatMapLatest { id ->
                if (id == null) flowOf(emptyList()) else conversations.observeMessages(id)
            }
            .onEach { messages -> _state.update { it.copy(messages = messages) } }
            .launchIn(viewModelScope)

        models.observeSelectedModel()
            .onEach { model -> _state.update { it.copy(selectedModel = model) } }
            .launchIn(viewModelScope)

        viewModelScope.launch {
            val available = models.availableModels()
            _state.update { it.copy(availableModels = available) }
        }
    }

    fun onIntent(intent: ChatIntent) {
        when (intent) {
            is ChatIntent.InputChanged -> _state.update { it.copy(input = intent.text) }
            ChatIntent.Send -> send()
            ChatIntent.NewChat -> newChat()
            is ChatIntent.OpenConversation -> open(intent.id)
            is ChatIntent.RenameConversation -> viewModelScope.launch {
                conversations.renameConversation(intent.id, intent.title)
            }
            is ChatIntent.DeleteConversation -> delete(intent.id)
            is ChatIntent.SelectModel -> viewModelScope.launch {
                models.setSelectedModel(intent.model)
            }
        }
    }

    private fun send() {
        val conversationId = selectedId.value ?: return
        val text = _state.value.input.trim()
        if (text.isEmpty() || _state.value.isThinking) return

        _state.update { it.copy(input = "", isThinking = true, thinkingLabel = "Thinking…") }
        viewModelScope.launch {
            runCatching {
                sendMessage(conversationId, text) { status ->
                    val label = when (status) {
                        AgentStatus.Thinking -> "Thinking…"
                        is AgentStatus.Retrying ->
                            "Model busy — retrying (${status.attempt}/${status.maxAttempts})…"
                    }
                    _state.update { it.copy(thinkingLabel = label) }
                }
            }.onFailure { error ->
                _effects.send(ChatEffect.ShowError(error.message ?: "Something went wrong"))
            }
            _state.update { it.copy(isThinking = false) }
        }
    }

    private fun newChat() {
        viewModelScope.launch {
            selectedId.value = conversations.createConversation()
            _effects.send(ChatEffect.CloseDrawer)
        }
    }

    private fun open(id: String) {
        selectedId.value = id
        viewModelScope.launch { _effects.send(ChatEffect.CloseDrawer) }
    }

    private fun delete(id: String) {
        viewModelScope.launch {
            conversations.deleteConversation(id)
            if (selectedId.value == id) {
                val remaining = _state.value.conversations.filterNot { it.id == id }
                selectedId.value = remaining.firstOrNull()?.id ?: conversations.createConversation()
            }
        }
    }
}
