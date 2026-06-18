package `in`.singhangad.adkassistant.presentation.chat

import `in`.singhangad.adkassistant.domain.model.Author
import `in`.singhangad.adkassistant.domain.model.ChatMessage
import junit.framework.TestCase.assertEquals
import junit.framework.TestCase.assertNull
import junit.framework.TestCase.assertTrue
import org.junit.Test

/**
 * Pure state/model tests. The ViewModel itself drives Room + coroutine streams and
 * is exercised manually / via instrumentation.
 */
class ChatViewModelTest {

  @Test
  fun defaultState_isEmptyAndIdle() {
    val state = ChatState()
    assertTrue(state.messages.isEmpty())
    assertTrue(state.conversations.isEmpty())
    assertNull(state.currentConversationId)
    assertEquals("", state.input)
    assertEquals(false, state.isThinking)
  }

  @Test
  fun currentTitle_fallsBackWhenNoConversation() {
    assertEquals("ADK Assistant", ChatState().currentTitle)
  }

  @Test
  fun message_retainsAuthorAndText() {
    val message = ChatMessage(Author.USER, "What time is it in Tokyo?")
    assertEquals(Author.USER, message.author)
    assertEquals("What time is it in Tokyo?", message.text)
  }
}
