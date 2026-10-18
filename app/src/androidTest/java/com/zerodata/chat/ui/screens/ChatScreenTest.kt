package com.zerodata.chat.ui.screens

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.zerodata.chat.model.Message
import com.zerodata.chat.model.MessageStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import androidx.test.ext.junit.runners.AndroidJUnit4

@RunWith(AndroidJUnit4::class)
class ChatScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun chatScreen_displaysMessages() {
        // Given
        val messages = listOf(
            Message(chatId = "chat1", senderId = "user1", text = "Hello"),
            Message(chatId = "chat1", senderId = "me", text = "Hi there")
        )

        // When
        composeTestRule.setContent {
            ChatScreen(
                userId = "me",
                messages = messages,
                onSendMessage = {},
                onBack = {}
            )
        }

        // Then
        composeTestRule.onNodeWithText("Hello").assertIsDisplayed()
        composeTestRule.onNodeWithText("Hi there").assertIsDisplayed()
    }

    @Test
    fun chatScreen_sendButtonClick_callsCallback() {
        // Given
        var capturedText = ""
        composeTestRule.setContent {
            ChatScreen(
                userId = "me",
                messages = emptyList(),
                onSendMessage = { capturedText = it },
                onBack = {}
            )
        }

        // When
        composeTestRule.onNodeWithText("Сообщение...").performTextInput("Hello World")
        composeTestRule.onNodeWithContentDescription("Send").performClick()

        // Then
        assertEquals("Hello World", capturedText)
    }

    @Test
    fun chatScreen_emptySend_doesNotCallCallback() {
        var sent = false
        composeTestRule.setContent {
            ChatScreen(
                userId = "me",
                messages = emptyList(),
                onSendMessage = { sent = true },
                onBack = {},
            )
        }
        composeTestRule.onNodeWithContentDescription("Send").performClick()
        assertFalse(sent)
    }

    @Test
    fun chatScreen_back_callsOnBack() {
        var back = false
        composeTestRule.setContent {
            ChatScreen(
                userId = "me",
                messages = emptyList(),
                onSendMessage = {},
                onBack = { back = true },
            )
        }
        composeTestRule.onNodeWithContentDescription("Назад").performClick()
        assertTrue(back)
    }

    @Test
    fun chatScreen_ownMessages_showDeliveryTicks() {
        composeTestRule.setContent {
            ChatScreen(
                userId = "me",
                messages = listOf(
                    Message(chatId = "c", senderId = "me", text = "sent", status = MessageStatus.SENT),
                    Message(chatId = "c", senderId = "me", text = "delivered", status = MessageStatus.DELIVERED),
                    Message(chatId = "c", senderId = "me", text = "read", status = MessageStatus.READ),
                    Message(chatId = "c", senderId = "friend", text = "incoming"),
                ),
                onSendMessage = {},
                onBack = {},
            )
        }
        composeTestRule.onNodeWithContentDescription("SENT").assertIsDisplayed()
        composeTestRule.onNodeWithContentDescription("DELIVERED").assertIsDisplayed()
        composeTestRule.onNodeWithContentDescription("READ").assertIsDisplayed()
        composeTestRule.onNodeWithText("incoming").assertIsDisplayed()
    }
}
