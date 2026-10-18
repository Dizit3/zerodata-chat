package com.zerodata.chat.ui.screens

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.longClick
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTouchInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.zerodata.chat.model.Chat
import com.zerodata.chat.network.GitHubRelease
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ChatListScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun hub_showsIdAndNewChatFab() {
        setHub()
        composeTestRule.onNodeWithText("My ID: @me").assertIsDisplayed()
        composeTestRule.onNodeWithContentDescription("Новый чат").assertIsDisplayed()
    }

    @Test
    fun fab_opensDialog_andConfirmCreatesChat() {
        var created: String? = null
        setHub(onAddChatClick = { created = it })

        composeTestRule.onNodeWithContentDescription("Новый чат").performClick()
        composeTestRule.onNodeWithText("Новый чат").assertIsDisplayed()
        composeTestRule.onNodeWithText("Введите ID (напр. user_1234)").performTextInput("@friend")
        composeTestRule.onNodeWithText("Создать").performClick()

        assertEquals("@friend", created)
    }

    @Test
    fun fab_blankConfirm_doesNotCreate() {
        var created: String? = null
        setHub(onAddChatClick = { created = it })

        composeTestRule.onNodeWithContentDescription("Новый чат").performClick()
        composeTestRule.onNodeWithText("Создать").performClick()

        assertNull(created)
        composeTestRule.onNodeWithText("Новый чат").assertIsDisplayed()
    }

    @Test
    fun fab_cancel_closesDialog() {
        setHub()
        composeTestRule.onNodeWithContentDescription("Новый чат").performClick()
        composeTestRule.onNodeWithText("Отмена").performClick()
        composeTestRule.onNodeWithText("Создать").assertDoesNotExist()
    }

    @Test
    fun chatRow_click_passesChatId() {
        var clicked: String? = null
        setHub(
            chats = listOf(Chat(id = "friend_me", name = "friend", unreadCount = 3)),
            onChatClick = { clicked = it },
        )
        composeTestRule.onNodeWithText("friend").performClick()
        assertEquals("friend_me", clicked)
        composeTestRule.onNodeWithText("3").assertIsDisplayed()
        composeTestRule.onNodeWithText("Нет сообщений").assertIsDisplayed()
    }

    @Test
    fun lobbyIcon_invokesCallback() {
        var lobby = false
        setHub(onLobbyClick = { lobby = true })
        composeTestRule.onNodeWithContentDescription("Lobby").performClick()
        assertTrue(lobby)
    }

    @Test
    fun longPress_confirm_deletesChat() {
        var deleted: String? = null
        setHub(
            chats = listOf(Chat(id = "friend_me", name = "friend")),
            onChatDelete = { deleted = it },
        )
        composeTestRule.onNodeWithText("friend").performTouchInput { longClick() }
        composeTestRule.onNodeWithText("Удалить чат?").assertIsDisplayed()
        composeTestRule.onNodeWithText("Удалить").performClick()
        assertEquals("friend_me", deleted)
    }

    @Test
    fun longPress_cancel_doesNotDelete() {
        var deleted: String? = null
        setHub(
            chats = listOf(Chat(id = "friend_me", name = "friend")),
            onChatDelete = { deleted = it },
        )
        composeTestRule.onNodeWithText("friend").performTouchInput { longClick() }
        composeTestRule.onNodeWithText("Отмена").performClick()
        assertNull(deleted)
        composeTestRule.onNodeWithText("Удалить чат?").assertDoesNotExist()
    }

    @Test
    fun updateIcon_shownWhenReleasePresent() {
        setHub(updateAvailable = GitHubRelease(tagName = "v0.3.0-pre.5"))
        composeTestRule.onNodeWithContentDescription("Update Available").assertIsDisplayed()
    }

    private fun setHub(
        chats: List<Chat> = emptyList(),
        onChatClick: (String) -> Unit = {},
        onChatDelete: (String) -> Unit = {},
        onAddChatClick: (String) -> Unit = {},
        onLobbyClick: () -> Unit = {},
        updateAvailable: GitHubRelease? = null,
    ) {
        composeTestRule.setContent {
            ChatListScreen(
                userId = "@me",
                chats = chats,
                connectionStatus = true,
                onChatClick = onChatClick,
                onChatDelete = onChatDelete,
                onAddChatClick = onAddChatClick,
                onLobbyClick = onLobbyClick,
                updateAvailable = updateAvailable,
            )
        }
    }
}
