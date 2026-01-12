package com.zerodata.chat.viewmodel

import com.zerodata.chat.model.Message
import com.zerodata.chat.repository.ChatRepository
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class ChatViewModelTest {

    private val repository: ChatRepository = mockk(relaxed = true)
    private val chatId = "test_chat"
    
    private lateinit var viewModel: ChatViewModel

    @Before
    fun setup() {
        viewModel = ChatViewModel(repository, chatId)
    }

    @Test
    fun `viewModel observes messages from repository`() {
        // Given
        val mockMessages = listOf(Message(chatId = chatId, senderId = "other", text = "Hello"))
        every { repository.getMessages(chatId) } returns MutableStateFlow(mockMessages)

        // Then
        assertEquals(mockMessages, viewModel.messages.value)
    }

    @Test
    fun `when sendMessage is called then delegates to repository`() {
        // When
        viewModel.sendMessage("My message")

        // Then
        coVerify { repository.sendMessage(chatId, "My message") }
    }
}
