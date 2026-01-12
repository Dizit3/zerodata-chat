package com.zerodata.chat.ui.screens

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.zerodata.chat.model.Message
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
        assert(capturedText == "Hello World")
    }
}
