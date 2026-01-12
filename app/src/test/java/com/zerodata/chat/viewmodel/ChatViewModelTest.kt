package com.zerodata.chat.viewmodel

import com.zerodata.chat.model.Message
import com.zerodata.chat.repository.ChatRepository
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ChatViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private val repository: ChatRepository = mockk(relaxed = true)
    private val chatId = "test_chat"
    
    private lateinit var viewModel: ChatViewModel

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `viewModel observes messages from repository`() = runTest(testDispatcher) {
        // Given
        val mockMessages = listOf(Message(chatId = chatId, senderId = "other", text = "Hello"))
        every { repository.getMessages(chatId) } returns MutableStateFlow(mockMessages)
        viewModel = ChatViewModel(repository, chatId)

        // Then
        assertEquals(mockMessages, viewModel.messages.value)
    }

    @Test
    fun `when sendMessage is called then delegates to repository`() = runTest(testDispatcher) {
        // Given
        viewModel = ChatViewModel(repository, chatId)

        // When
        viewModel.sendMessage("My message")
        advanceUntilIdle()

        // Then
        coVerify { repository.sendMessage(chatId, "My message") }
    }
}
