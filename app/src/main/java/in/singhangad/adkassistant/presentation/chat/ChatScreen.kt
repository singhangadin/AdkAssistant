package `in`.singhangad.adkassistant.presentation.chat

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import `in`.singhangad.adkassistant.domain.model.Author
import `in`.singhangad.adkassistant.domain.model.ChatMessage
import kotlinx.coroutines.launch

@Composable
fun ChatScreen(
    onAbout: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ChatViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        viewModel.effects.collect { effect ->
            when (effect) {
                is ChatEffect.ShowError -> snackbarHostState.showSnackbar(effect.message)
                ChatEffect.CloseDrawer -> drawerState.close()
            }
        }
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            HistoryDrawer(
                conversations = state.conversations,
                currentId = state.currentConversationId,
                onNewChat = { viewModel.onIntent(ChatIntent.NewChat) },
                onOpen = { viewModel.onIntent(ChatIntent.OpenConversation(it)) },
                onRename = { id, title -> viewModel.onIntent(ChatIntent.RenameConversation(id, title)) },
                onDelete = { viewModel.onIntent(ChatIntent.DeleteConversation(it)) },
            )
        },
    ) {
        ChatContent(
            state = state,
            onIntent = viewModel::onIntent,
            onAbout = onAbout,
            onOpenDrawer = { scope.launch { drawerState.open() } },
            snackbarHostState = snackbarHostState,
            modifier = modifier,
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ChatContent(
    state: ChatState,
    onIntent: (ChatIntent) -> Unit,
    onAbout: () -> Unit,
    onOpenDrawer: () -> Unit,
    snackbarHostState: SnackbarHostState,
    modifier: Modifier = Modifier,
) {
    val listState = rememberLazyListState()

    LaunchedEffect(state.messages.size, state.isThinking) {
        val count = state.messages.size + if (state.isThinking) 1 else 0
        if (count > 0) listState.animateScrollToItem(count - 1)
    }

    Scaffold(
        modifier = modifier,
        // Let the Scaffold own ALL insets — system bars AND the IME — in one place.
        // The content padding then already lifts the input above the keyboard, and
        // the TopAppBar stays pinned. (No separate imePadding → no double counting.)
        contentWindowInsets = WindowInsets.safeDrawing,
        topBar = {
            TopAppBar(
                title = { Text(state.currentTitle, maxLines = 1) },
                navigationIcon = { TextButton(onClick = onOpenDrawer) { Text("☰") } },
                actions = {
                    ModelMenu(
                        selected = state.selectedModel,
                        available = state.availableModels,
                        onSelect = { onIntent(ChatIntent.SelectModel(it)) },
                    )
                    TextButton(onClick = onAbout) { Text("About") }
                },
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .consumeWindowInsets(padding)
                .padding(horizontal = 16.dp)
        ) {
            LazyColumn(
                state = listState,
                modifier = Modifier.weight(1f).fillMaxWidth(),
                // Anchor messages to the bottom so they stack upward, chat-style.
                verticalArrangement = Arrangement.spacedBy(8.dp, Alignment.Bottom),
            ) {
                items(state.messages) { message -> MessageBubble(message) }
                if (state.isThinking) {
                    item { ThinkingBubble(label = state.thinkingLabel) }
                }
            }

            InputBar(
                input = state.input,
                enabled = !state.isThinking,
                onInputChange = { onIntent(ChatIntent.InputChanged(it)) },
                onSend = { onIntent(ChatIntent.Send) },
            )
        }
    }
}

@Composable
private fun InputBar(
    input: String,
    enabled: Boolean,
    onInputChange: (String) -> Unit,
    onSend: () -> Unit,
) {
    androidx.compose.foundation.layout.Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        OutlinedTextField(
            value = input,
            onValueChange = onInputChange,
            modifier = Modifier.weight(1f),
            placeholder = { Text("Ask the agent…") },
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
        )
        Button(
            onClick = onSend,
            enabled = input.isNotBlank() && enabled,
            modifier = Modifier.padding(start = 8.dp),
        ) {
            Text("Send")
        }
    }
}

@Composable
private fun ModelMenu(selected: String, available: List<String>, onSelect: (String) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    // Always include the current selection so it's pickable even before the list loads.
    val options = (listOf(selected) + available).filter { it.isNotBlank() }.distinct()

    Box {
        TextButton(onClick = { expanded = true }, enabled = options.isNotEmpty()) {
            Text(selected.ifBlank { "model" }.removePrefix("gemini-") + " ▾", maxLines = 1)
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            options.forEach { model ->
                DropdownMenuItem(
                    text = { Text(if (model == selected) "$model  ✓" else model) },
                    onClick = {
                        expanded = false
                        if (model != selected) onSelect(model)
                    },
                )
            }
        }
    }
}

@Composable
private fun ThinkingBubble(label: String) {
    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.CenterStart) {
        Surface(
            color = MaterialTheme.colorScheme.surfaceVariant,
            shape = RoundedCornerShape(16.dp),
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                Spacer(Modifier.width(8.dp))
                Text(label)
            }
        }
    }
}

@Composable
private fun MessageBubble(message: ChatMessage) {
    val fromUser = message.author == Author.USER
    val color =
        if (fromUser) MaterialTheme.colorScheme.primaryContainer
        else MaterialTheme.colorScheme.surfaceVariant
    Box(
        modifier = Modifier.fillMaxWidth(),
        contentAlignment = if (fromUser) Alignment.CenterEnd else Alignment.CenterStart,
    ) {
        Surface(
            color = color,
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.widthIn(max = 300.dp),
        ) {
            Text(text = message.text, modifier = Modifier.padding(12.dp))
        }
    }
}
