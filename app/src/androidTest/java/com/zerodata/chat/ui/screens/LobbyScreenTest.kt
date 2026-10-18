package com.zerodata.chat.ui.screens

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class LobbyScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun emptyLobby_showsSearching() {
        composeTestRule.setContent {
            LobbyContent(users = emptyList(), onUserClick = {}, onBack = {})
        }
        composeTestRule.onNodeWithText("Поиск пользователей...").assertIsDisplayed()
        composeTestRule.onNodeWithText("Глобальное лобби").assertIsDisplayed()
    }

    @Test
    fun userRow_click_passesId() {
        var clicked: String? = null
        composeTestRule.setContent {
            LobbyContent(
                users = listOf("@friend"),
                onUserClick = { clicked = it },
                onBack = {},
            )
        }
        composeTestRule.onNodeWithText("@friend").performClick()
        assertEquals("@friend", clicked)
    }

    @Test
    fun back_callsOnBack() {
        var back = false
        composeTestRule.setContent {
            LobbyContent(users = emptyList(), onUserClick = {}, onBack = { back = true })
        }
        composeTestRule.onNodeWithContentDescription("Назад").performClick()
        assertTrue(back)
    }
}
