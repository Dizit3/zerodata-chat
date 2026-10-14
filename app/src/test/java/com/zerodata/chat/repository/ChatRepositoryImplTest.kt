package com.zerodata.chat.repository

import com.zerodata.chat.database.MessageEntity
import com.zerodata.chat.model.Message
import com.zerodata.chat.model.MessageStatus
import com.zerodata.chat.model.MessageType
import com.zerodata.chat.network.MqttManager
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ChatRepositoryImplTest {

    private val testDispatcher = StandardTestDispatcher()
    private val testScope = TestScope(testDispatcher)
    private val mqttManager: MqttManager = mockk(relaxed = true)
    private val chatDao: com.zerodata.chat.database.ChatDao = mockk(relaxed = true)
    private val messageDao: com.zerodata.chat.database.MessageDao = mockk(relaxed = true)
    private val incomingMessagesFlow = MutableSharedFlow<Message>(extraBufferCapacity = 64)
    private val connectionStatus = MutableStateFlow(false)
    
    private val currentUserId = "me"
    private lateinit var repository: ChatRepositoryImpl

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        every { mqttManager.observeMessages() } returns incomingMessagesFlow
        every { mqttManager.connectionStatus } returns connectionStatus
        every { chatDao.getChatsWithLastMessageFlow() } returns flowOf(emptyList())
        every { messageDao.getMessagesForChatFlow(any()) } returns flowOf(emptyList())
        coEvery { messageDao.getOutgoingPending(any(), any()) } returns emptyList()
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
    fun `when sendMessage succeeds then status is SENT`() = testScope.runTest {
        repository.sendMessage("friend", "Hi")
        advanceUntilIdle()

        coVerify { messageDao.insertMessage(match { it.text == "Hi" && it.status == MessageStatus.SENDING }) }
        coVerify { mqttManager.sendMessage(match { it.text == "Hi" && it.receiverId == "friend" }) }
        coVerify { messageDao.updateStatus(any(), MessageStatus.SENT) }

        coroutineContext.cancelChildren()
    }

    @Test
    fun `when sendMessage fails then status is ERROR`() = testScope.runTest {
        coEvery { mqttManager.sendMessage(any()) } throws IllegalStateException("MQTT not connected")

        repository.sendMessage("friend", "Hi")
        advanceUntilIdle()

        coVerify { messageDao.insertMessage(match { it.text == "Hi" && it.status == MessageStatus.SENDING }) }
        coVerify { messageDao.updateStatus(any(), MessageStatus.ERROR) }
        coVerify(exactly = 0) { messageDao.updateStatus(any(), MessageStatus.SENT) }

        coroutineContext.cancelChildren()
    }

    @Test
    fun `when mqtt reconnects then ERROR and SENDING outgoing are retried`() = testScope.runTest {
        val pending = listOf(
            MessageEntity(
                id = "err-1",
                chatId = "friend_me",
                senderId = currentUserId,
                receiverId = "friend",
                text = "offline",
                timestamp = 1L,
                status = MessageStatus.ERROR,
                type = MessageType.TEXT
            ),
            MessageEntity(
                id = "snd-1",
                chatId = "friend_me",
                senderId = currentUserId,
                receiverId = "friend",
                text = "queued",
                timestamp = 2L,
                status = MessageStatus.SENDING,
                type = MessageType.TEXT
            )
        )
        coEvery { messageDao.getOutgoingPending(currentUserId, any()) } returns pending

        connectionStatus.value = true
        advanceUntilIdle()

        coVerify { messageDao.updateStatus("err-1", MessageStatus.SENDING) }
        coVerify { mqttManager.sendMessage(match { it.id == "err-1" }) }
        coVerify { mqttManager.sendMessage(match { it.id == "snd-1" }) }
        coVerify { messageDao.updateStatus("err-1", MessageStatus.SENT) }
        coVerify { messageDao.updateStatus("snd-1", MessageStatus.SENT) }

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

    @Test
    fun `when createChat is called then canonical ID is used`() = testScope.runTest {
        // When
        repository.createChat("friend")
        advanceUntilIdle()

        // Then
        // ID should be min(me, friend)_max(me, friend) -> "friend_me"
        coVerify { chatDao.insertChat(match { it.id == "friend_me" }) }
        coroutineContext.cancelChildren()
    }

    @Test
    fun `when incoming message received then it is mapped to canonical chatId`() = testScope.runTest {
        // Given
        val message = Message(chatId = "some_random_id", senderId = "friend", text = "Hello", receiverId = "me")

        // When
        incomingMessagesFlow.emit(message)
        advanceUntilIdle()

        // Then
        // Should match "friend_me" (alphabetical order of "friend" and "me")
        coVerify { chatDao.insertChat(match { it.id == "friend_me" }) } 
        coVerify { messageDao.insertMessage(match { it.chatId == "friend_me" }) }

        coroutineContext.cancelChildren()
    }
}
