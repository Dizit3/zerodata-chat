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
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test
import kotlinx.coroutines.flow.flowOf
import com.zerodata.chat.database.ChatEntity
import com.zerodata.chat.database.MessageEntity

@OptIn(ExperimentalCoroutinesApi::class)
class ChatRepositoryImplTest {

    private val testDispatcher = StandardTestDispatcher()
    private val testScope = TestScope(testDispatcher)
    private val mqttManager: MqttManager = mockk(relaxed = true)
    private val chatDao: com.zerodata.chat.database.ChatDao = mockk(relaxed = true)
    private val messageDao: com.zerodata.chat.database.MessageDao = mockk(relaxed = true)
    private val incomingMessagesFlow = MutableSharedFlow<Message>(extraBufferCapacity = 64)
    
    private val currentUserId = "me"
    private lateinit var repository: ChatRepositoryImpl

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        every { mqttManager.observeMessages() } returns incomingMessagesFlow
        every { chatDao.getChatsWithLastMessageFlow() } returns flowOf(emptyList())
        every { messageDao.getMessagesForChatFlow(any()) } returns flowOf(emptyList())
        repository = ChatRepositoryImpl(mqttManager, currentUserId, chatDao, messageDao, testScope)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `when message is received then it is handled via DAO`() = testScope.runTest {
        // Given
        val message = Message(chatId = "friend", senderId = "friend", text = "Hello")

        // When
        incomingMessagesFlow.emit(message)
        advanceUntilIdle()

        // Then
        coVerify { messageDao.insertMessage(any()) }
        coVerify { chatDao.insertChat(any()) } // because chat doesn't exist in our empty mock

        coroutineContext.cancelChildren()
    }

    @Test
    fun `when sendMessage is called then message is inserted and sent via MQTT`() = testScope.runTest {
        // When
        repository.sendMessage("friend", "Hi")
        advanceUntilIdle()

        // Then
        coVerify { messageDao.insertMessage(match { it.text == "Hi" }) }
        coVerify { mqttManager.sendMessage(match { it.text == "Hi" && it.receiverId == "friend" }) }

        coroutineContext.cancelChildren()
    }

    @Test
    fun `when clearUnread is called then DAO is notified`() = testScope.runTest {
        // When
        repository.clearUnread("friend")
        advanceUntilIdle()

        // Then
        coVerify { chatDao.updateUnreadCount("friend", 0) }

        coroutineContext.cancelChildren()
    }
}
