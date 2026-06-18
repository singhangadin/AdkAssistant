package com.example.adkassistant.ui.main

import junit.framework.TestCase.assertEquals
import junit.framework.TestCase.assertTrue
import org.junit.Test

/**
 * Pure-state tests. The conversation itself calls a network model, so it is
 * exercised with the instrumented/manual flow rather than here.
 */
class MainScreenViewModelTest {

  @Test
  fun defaultState_isEmptyAndIdle() {
    val state = ChatUiState()
    assertTrue(state.messages.isEmpty())
    assertEquals(false, state.isThinking)
  }

  @Test
  fun message_retainsAuthorAndText() {
    val message = ChatMessage(Author.USER, "What time is it in Tokyo?")
    assertEquals(Author.USER, message.author)
    assertEquals("What time is it in Tokyo?", message.text)
  }
}
