package com.example.adkassistant.ui.main

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import org.junit.Rule
import org.junit.Test

/** UI test for [ChatScreen] rendering with a static conversation. */
class MainScreenTest {

  @get:Rule val composeTestRule = createAndroidComposeRule<ComponentActivity>()

  @Test
  fun messages_areDisplayed() {
    val state = ChatUiState(
      messages = listOf(
        ChatMessage(Author.USER, "What time is it in Tokyo?"),
        ChatMessage(Author.AGENT, "It's 9:30 PM in Tokyo."),
      )
    )
    composeTestRule.setContent { ChatScreen(state = state, onSend = {}) }

    composeTestRule.onNodeWithText("What time is it in Tokyo?").assertExists()
    composeTestRule.onNodeWithText("It's 9:30 PM in Tokyo.").assertExists()
  }
}
