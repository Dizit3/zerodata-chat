package com.zerodata.chat.repository

import com.zerodata.chat.model.Message
import com.zerodata.chat.network.MqttManager
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ChatRepositoryImplTest {

    private val testDispatcher = StandardTestDispatcher()
    private val testScope = TestScope(testDispatcher)
    private val mqttManager: MqttManager = mockk(relaxed = true)
    private val incomingMessagesFlow = MutableSharedFlow<Message>()
    
    private val currentUserId = "me"
    private lateinit var repository: ChatRepositoryImpl

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        every { mqttManager.observeMessages() } returns incomingMessagesFlow
        repository = ChatRepositoryImpl(mqttManager, currentUserId, testScope)
    }

    @After
    fun tearDown() {
        testScope.coroutineContext.cancelChildren()
    }

    @Test
    fun `when message is received then it is added to the correct chat`() = testScope.runTest {
        // Given
        val message = Message(chatId = "friend", senderId = "friend", text = "Hello")

        // When
        incomingMessagesFlow.emit(message)
        advanceUntilIdle()

        // Then
        val chats = repository.allChats.value
        assertEquals(1, chats.size)
        assertEquals("friend", chats[0].id)
        assertEquals(1, chats[0].unreadCount)
        
        val messages = repository.getMessages("friend").value
        assertEquals(1, messages.size)
        assertEquals("Hello", messages[0].text)
    }

    @Test
    fun `when sendMessage is called then message is added locally and sent via MQTT`() = testScope.runTest {
        // When
        repository.sendMessage("friend", "Hi")
        advanceUntilIdle()

        // Then
        val messages = repository.getMessages("friend").value
        assertEquals(1, messages.size)
        assertEquals("Hi", messages[0].text)
        
        coVerify { mqttManager.sendMessage(match { it.text == "Hi" && it.receiverId == "friend" }) }
    }

    @Test
    fun `when clearUnread is called then unread count is reset`() = testScope.runTest {
        // Given
        incomingMessagesFlow.emit(Message(chatId = "friend", senderId = "friend", text = "Msg"))
        advanceUntilIdle()
        assertEquals(1, repository.allChats.value[0].unreadCount)

        // When
        repository.clearUnread("friend")

        // Then
        assertEquals(0, repository.allChats.value[0].unreadCount)
    }
}
