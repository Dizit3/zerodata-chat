package com.zerodata.chat.viewmodel

import com.zerodata.chat.model.Chat
import com.zerodata.chat.repository.ChatRepository
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class MainViewModelTest {

    private lateinit var repository: ChatRepository
    private lateinit var viewModel: MainViewModel

    @Before
    fun setup() {
        repository = mockk(relaxed = true)
    }

    @Test
    fun `viewModel observes allChats from repository`() {
        // Given
        val mockChats = listOf(Chat(id = "chat1", name = "Chat 1"))
        every { repository.allChats } returns MutableStateFlow(mockChats)

        // When
        viewModel = MainViewModel(repository)

        // Then
        assertEquals(mockChats, viewModel.chats.value)
    }

    @Test
    fun `when createChat is called then delegates to repository`() {
        // Given
        viewModel = MainViewModel(repository)

        // When
        viewModel.createChat("user123")

        // Then
        verify { repository.createChat("user123") }
    }

    @Test
    fun `when clearUnread is called then delegates to repository`() {
        // Given
        viewModel = MainViewModel(repository)

        // When
        viewModel.clearUnread("chat1")

        // Then
        verify { repository.clearUnread("chat1") }
    }
}
