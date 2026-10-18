package com.zerodata.chat.repository

import com.zerodata.chat.database.ChatEntity
import com.zerodata.chat.database.ChatWithLastMessage
import com.zerodata.chat.database.MessageEntity
import com.zerodata.chat.model.Message
import com.zerodata.chat.model.MessageStatus
import com.zerodata.chat.model.MessageType
import com.zerodata.chat.network.MqttInbound
import com.zerodata.chat.network.MqttManager
import com.zerodata.chat.network.ReceiptKind
import com.zerodata.chat.util.Constants
import com.zerodata.chat.util.IncomingMessageNotifier
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
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
    private val inboundFlow = MutableSharedFlow<MqttInbound>(extraBufferCapacity = 64)
    private val connectionStatus = MutableStateFlow(false)
    private val chatsWithLastMessage = MutableStateFlow<List<ChatWithLastMessage>>(emptyList())

    private val currentUserId = "me"
    private val incomingNotifier: IncomingMessageNotifier = mockk(relaxed = true)
    private var nowMs = 1_000_000L
    private lateinit var repository: ChatRepositoryImpl

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        chatsWithLastMessage.value = emptyList()
        every { mqttManager.observeInbound() } returns inboundFlow
        every { mqttManager.connectionStatus } returns connectionStatus
        every { chatDao.getChatsWithLastMessageFlow() } returns chatsWithLastMessage
        every { messageDao.getMessagesForChatFlow(any()) } returns flowOf(emptyList())
        coEvery { messageDao.getOutgoingPending(any(), any()) } returns emptyList()
        nowMs = 1_000_000L
        repository = ChatRepositoryImpl(
            mqttManager,
            currentUserId,
            chatDao,
            messageDao,
            incomingNotifier,
            testScope,
        ) { nowMs }
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
        inboundFlow.emit(MqttInbound.ChatMessage(message))
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
        inboundFlow.emit(MqttInbound.ChatMessage(message))
        advanceUntilIdle()

        // Then
        // Should match "friend_me" (alphabetical order of "friend" and "me")
        coVerify { chatDao.insertChat(match { it.id == "friend_me" }) } 
        coVerify { messageDao.insertMessage(match { it.chatId == "friend_me" }) }

        coroutineContext.cancelChildren()
    }

    @Test
    fun `when sendMessage uses canonical chatId then receiver is the other participant`() = testScope.runTest {
        repository.sendMessage("friend_me", "Hi")
        advanceUntilIdle()

        coVerify {
            mqttManager.sendMessage(
                match { it.receiverId == "friend" && it.senderId == currentUserId && it.text == "Hi" }
            )
        }
        coroutineContext.cancelChildren()
    }

    @Test
    fun `when incoming message for existing chat then unread increments`() = testScope.runTest {
        seedExistingChat(unread = 2)
        launch { repository.allChats.collect { } }
        advanceUntilIdle()

        inboundFlow.emit(
            MqttInbound.ChatMessage(
                Message(
                    chatId = "ignored",
                    senderId = "friend",
                    receiverId = currentUserId,
                    text = "Hello",
                )
            )
        )
        advanceUntilIdle()

        coVerify { chatDao.updateUnreadCount("friend_me", 3) }
        coVerify(exactly = 0) { chatDao.insertChat(any()) }

        coroutineContext.cancelChildren()
    }

    @Test
    fun `when outgoing message for existing chat then unread stays`() = testScope.runTest {
        seedExistingChat(unread = 2)
        launch { repository.allChats.collect { } }
        advanceUntilIdle()

        repository.sendMessage("friend_me", "Hi")
        advanceUntilIdle()

        coVerify(exactly = 0) { chatDao.updateUnreadCount(any(), any()) }
        coVerify(exactly = 0) { chatDao.insertChat(any()) }

        coroutineContext.cancelChildren()
    }

    @Test
    fun `when retry publish fails then status is ERROR`() = testScope.runTest {
        val pending = listOf(
            MessageEntity(
                id = "err-2",
                chatId = "friend_me",
                senderId = currentUserId,
                receiverId = "friend",
                text = "offline",
                timestamp = 1L,
                status = MessageStatus.ERROR,
                type = MessageType.TEXT
            )
        )
        coEvery { messageDao.getOutgoingPending(currentUserId, any()) } returns pending
        coEvery { mqttManager.sendMessage(any()) } throws IllegalStateException("MQTT not connected")

        connectionStatus.value = true
        advanceUntilIdle()

        coVerify { messageDao.updateStatus("err-2", MessageStatus.SENDING) }
        coVerify { mqttManager.sendMessage(match { it.id == "err-2" }) }
        coVerify { messageDao.updateStatus("err-2", MessageStatus.ERROR) }

        coroutineContext.cancelChildren()
    }

    @Test
    fun `incoming chat sends delivery receipt`() = testScope.runTest {
        val message = Message(
            id = "in-1",
            chatId = "friend_me",
            senderId = "friend",
            receiverId = currentUserId,
            text = "Hello",
        )
        inboundFlow.emit(MqttInbound.ChatMessage(message))
        advanceUntilIdle()

        coVerify { mqttManager.sendReceipt("friend", ReceiptKind.DELIVERED, "in-1") }
        verify {
            incomingNotifier.notifyIncoming("friend_me", "friend", "Hello")
        }

        coroutineContext.cancelChildren()
    }

    @Test
    fun `incoming chat while active sends read receipt`() = testScope.runTest {
        coEvery { messageDao.getIncomingForChat(any(), any()) } returns emptyList()
        repository.setActiveChat("friend_me")
        advanceUntilIdle()

        val message = Message(
            id = "in-2",
            chatId = "friend_me",
            senderId = "friend",
            receiverId = currentUserId,
            text = "Hello",
        )
        inboundFlow.emit(MqttInbound.ChatMessage(message))
        advanceUntilIdle()

        coVerify { mqttManager.sendReceipt("friend", ReceiptKind.READ, "in-2") }
        verify(exactly = 0) { incomingNotifier.notifyIncoming(any(), any(), any()) }

        coroutineContext.cancelChildren()
    }

    @Test
    fun `opening a chat cancels its incoming notification`() = testScope.runTest {
        coEvery { messageDao.getIncomingForChat(any(), any()) } returns emptyList()
        repository.setActiveChat("friend_me")
        advanceUntilIdle()
        verify { incomingNotifier.cancel("friend_me") }
        coroutineContext.cancelChildren()
    }

    @Test
    fun `deleteChat removes messages then chat`() = testScope.runTest {
        repository.deleteChat("friend_me")
        advanceUntilIdle()
        verify { incomingNotifier.cancel("friend_me") }
        coVerify { messageDao.deleteMessagesForChat("friend_me") }
        coVerify { chatDao.deleteChat("friend_me") }
        coroutineContext.cancelChildren()
    }

    @Test
    fun `delivery receipt upgrades SENT to DELIVERED`() = testScope.runTest {
        coEvery { messageDao.getById("out-1") } returns MessageEntity(
            id = "out-1",
            chatId = "friend_me",
            senderId = currentUserId,
            receiverId = "friend",
            text = "Hi",
            timestamp = 1L,
            status = MessageStatus.SENT,
            type = MessageType.TEXT,
        )

        inboundFlow.emit(MqttInbound.Receipt(ReceiptKind.DELIVERED, "out-1"))
        advanceUntilIdle()

        coVerify { messageDao.updateStatus("out-1", MessageStatus.DELIVERED) }
        coVerify(exactly = 0) { messageDao.insertMessage(any()) }

        coroutineContext.cancelChildren()
    }

    @Test
    fun `read receipt upgrades past delivered`() = testScope.runTest {
        coEvery { messageDao.getById("out-2") } returns MessageEntity(
            id = "out-2",
            chatId = "friend_me",
            senderId = currentUserId,
            receiverId = "friend",
            text = "Hi",
            timestamp = 1L,
            status = MessageStatus.DELIVERED,
            type = MessageType.TEXT,
        )

        inboundFlow.emit(MqttInbound.Receipt(ReceiptKind.READ, "out-2"))
        advanceUntilIdle()

        coVerify { messageDao.updateStatus("out-2", MessageStatus.READ) }

        coroutineContext.cancelChildren()
    }

    @Test
    fun `unknown receipt id is ignored`() = testScope.runTest {
        coEvery { messageDao.getById("missing") } returns null

        inboundFlow.emit(MqttInbound.Receipt(ReceiptKind.DELIVERED, "missing"))
        advanceUntilIdle()

        coVerify(exactly = 0) { messageDao.updateStatus("missing", any()) }

        coroutineContext.cancelChildren()
    }

    @Test
    fun `receipt does not downgrade READ`() = testScope.runTest {
        coEvery { messageDao.getById("out-3") } returns MessageEntity(
            id = "out-3",
            chatId = "friend_me",
            senderId = currentUserId,
            receiverId = "friend",
            text = "Hi",
            timestamp = 1L,
            status = MessageStatus.READ,
            type = MessageType.TEXT,
        )

        inboundFlow.emit(MqttInbound.Receipt(ReceiptKind.DELIVERED, "out-3"))
        advanceUntilIdle()

        coVerify(exactly = 0) { messageDao.updateStatus("out-3", MessageStatus.DELIVERED) }

        coroutineContext.cancelChildren()
    }

    @Test
    fun `ensureConnected skips while MQTT is up`() = testScope.runTest {
        connectionStatus.value = true
        advanceUntilIdle()
        nowMs += Constants.MQTT_RECONNECT_MIN_INTERVAL_MS
        repository.ensureConnected()
        advanceUntilIdle()
        coVerify(exactly = 1) { mqttManager.connect() }
        coroutineContext.cancelChildren()
    }

    @Test
    fun `ensureConnected reconnects after min interval when down`() = testScope.runTest {
        advanceUntilIdle()
        nowMs += Constants.MQTT_RECONNECT_MIN_INTERVAL_MS
        repository.ensureConnected()
        advanceUntilIdle()
        coVerify(exactly = 2) { mqttManager.connect() }
        coroutineContext.cancelChildren()
    }

    private fun seedExistingChat(unread: Int) {
        chatsWithLastMessage.value = listOf(
            ChatWithLastMessage(
                ChatEntity(
                    id = "friend_me",
                    name = "friend",
                    unreadCount = unread,
                ),
                emptyList(),
            )
        )
    }
}
