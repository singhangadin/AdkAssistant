package `in`.singhangad.adkassistant.presentation.chat

import androidx.activity.ComponentActivity
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.remember
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import `in`.singhangad.adkassistant.domain.model.Author
import `in`.singhangad.adkassistant.domain.model.ChatMessage
import org.junit.Rule
import org.junit.Test

/** Renders the stateless [ChatContent] with a static conversation. */
class ChatScreenTest {

  @get:Rule val composeTestRule = createAndroidComposeRule<ComponentActivity>()

  @Test
  fun messages_areDisplayed() {
    val state = ChatState(
      messages = listOf(
        ChatMessage(Author.USER, "What time is it in Tokyo?"),
        ChatMessage(Author.AGENT, "It's 9:30 PM in Tokyo."),
      )
    )
    composeTestRule.setContent {
      ChatContent(
        state = state,
        onIntent = {},
        onAbout = {},
        onOpenDrawer = {},
        snackbarHostState = remember { SnackbarHostState() },
      )
    }

    composeTestRule.onNodeWithText("What time is it in Tokyo?").assertExists()
    composeTestRule.onNodeWithText("It's 9:30 PM in Tokyo.").assertExists()
  }
}
