package com.zerodata.chat.repository

import com.zerodata.chat.database.ChatDao
import com.zerodata.chat.database.ChatEntity
import com.zerodata.chat.database.MessageDao
import com.zerodata.chat.database.MessageEntity
import com.zerodata.chat.model.Chat
import com.zerodata.chat.model.Message
import com.zerodata.chat.model.MessageStatus
import com.zerodata.chat.model.deliveryRank
import com.zerodata.chat.network.MqttInbound
import com.zerodata.chat.network.MqttMessagingManager
import com.zerodata.chat.network.ReceiptKind
import com.zerodata.chat.util.ChatUtils
import com.zerodata.chat.util.Constants
import com.zerodata.chat.util.IncomingMessageNotifier
import com.zerodata.chat.util.IncomingNotificationPolicy
import com.zerodata.chat.util.MqttWatchdog
import com.zerodata.chat.util.NoOpIncomingMessageNotifier
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

/**
 * Реализация репозитория с использованием Room для персистентного хранения.
 */
class ChatRepositoryImpl(
    private val mqttManager: MqttMessagingManager,
    private val currentUserId: String,
    private val chatDao: ChatDao,
    private val messageDao: MessageDao,
    private val incomingNotifier: IncomingMessageNotifier = NoOpIncomingMessageNotifier(),
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Main),
    private val nowMillis: () -> Long = { System.currentTimeMillis() },
) : ChatRepository {

    override val allChats: StateFlow<List<Chat>> = chatDao.getChatsWithLastMessageFlow()
        .map { list ->
            list.map { item ->
                Chat(
                    id = item.chat.id,
                    name = item.chat.name,
                    unreadCount = item.chat.unreadCount,
                    avatarUrl = item.chat.avatarUrl,
                    lastMessage = item.lastMessage?.toModel()
                )
            }
        }
        .stateIn(scope, SharingStarted.Lazily, emptyList())

    private val _connectionStatus = MutableStateFlow(false)
    override val connectionStatus: StateFlow<Boolean> = _connectionStatus.asStateFlow()

    @Volatile
    private var activeChatId: String? = null

    @Volatile
    private var lastConnectAttemptMs: Long = 0L

    init {
        lastConnectAttemptMs = nowMillis()
        scope.launch {
            mqttManager.connect()
        }

        // Наблюдаем за подключением и досылаем очередь при (ре)коннекте
        scope.launch {
            mqttManager.connectionStatus.collect { connected ->
                _connectionStatus.value = connected
                if (connected) {
                    retryOutgoing()
                }
            }
        }

        scope.launch {
            mqttManager.observeInbound().collect { inbound ->
                when (inbound) {
                    is MqttInbound.ChatMessage -> handleIncomingMessage(inbound.message)
                    is MqttInbound.Receipt -> applyReceipt(inbound)
                }
            }
        }
    }

    override fun getMessages(chatId: String): StateFlow<List<Message>> {
        return messageDao.getMessagesForChatFlow(chatId)
            .map { entities -> entities.map { it.toModel() } }
            .stateIn(scope, SharingStarted.Lazily, emptyList())
    }

    override suspend fun sendMessage(chatId: String, text: String) {
        // Extract recipient from the composite chatId (format: id1_id2)
        // If chatId doesn't contain the separator (legacy/migration), assume it is the recipientId
        val recipientId = if (chatId.contains("_")) {
            chatId.split("_").firstOrNull { it != currentUserId } ?: chatId
        } else {
            chatId
        }

        val message = Message(
            chatId = chatId,
            senderId = currentUserId,
            receiverId = recipientId,
            text = text,
            status = MessageStatus.SENDING
        )

        saveMessageAndHandleChat(message)
        publishOutgoing(message)
    }

    override fun createChat(recipientId: String) {
        scope.launch {
            val chatId = ChatUtils.getCanonicalChatId(currentUserId, recipientId)
            chatDao.insertChat(
                ChatEntity(
                    id = chatId,
                    name = recipientId, // TODO: Fetch real name if possible
                    unreadCount = 0
                )
            )
        }
    }

    override fun deleteChat(chatId: String) {
        incomingNotifier.cancel(chatId)
        if (activeChatId == chatId) {
            activeChatId = null
        }
        scope.launch {
            messageDao.deleteMessagesForChat(chatId)
            chatDao.deleteChat(chatId)
        }
    }

    override fun clearUnread(chatId: String) {
        scope.launch {
            chatDao.updateUnreadCount(chatId, 0)
        }
    }

    override fun ensureConnected() {
        val now = nowMillis()
        if (!MqttWatchdog.shouldAttempt(
                _connectionStatus.value,
                now - lastConnectAttemptMs,
                Constants.MQTT_RECONNECT_MIN_INTERVAL_MS,
            )
        ) {
            return
        }
        lastConnectAttemptMs = now
        scope.launch {
            mqttManager.connect()
        }
    }

    override fun setActiveChat(chatId: String?) {
        activeChatId = chatId
        if (chatId != null) {
            incomingNotifier.cancel(chatId)
            scope.launch {
                chatDao.updateUnreadCount(chatId, 0)
                sendReadReceipts(chatId)
            }
        }
    }

    private suspend fun handleIncomingMessage(message: Message) {
        saveMessageAndHandleChat(message)
        if (message.senderId == currentUserId) return
        val chatId = ChatUtils.getCanonicalChatId(message.senderId, message.receiverId)
        val kind = if (activeChatId == chatId) ReceiptKind.READ else ReceiptKind.DELIVERED
        mqttManager.sendReceipt(message.senderId, kind, message.id)
        if (IncomingNotificationPolicy.shouldNotify(
                senderId = message.senderId,
                currentUserId = currentUserId,
                chatId = chatId,
                activeChatId = activeChatId,
            )
        ) {
            incomingNotifier.notifyIncoming(chatId, message.senderId, message.text)
        }
    }

    private suspend fun applyReceipt(receipt: MqttInbound.Receipt) {
        val existing = messageDao.getById(receipt.messageId) ?: return
        if (existing.senderId != currentUserId) return
        if (existing.status == MessageStatus.ERROR) return
        val next = when (receipt.kind) {
            ReceiptKind.DELIVERED -> MessageStatus.DELIVERED
            ReceiptKind.READ -> MessageStatus.READ
        }
        if (next.deliveryRank() > existing.status.deliveryRank()) {
            messageDao.updateStatus(existing.id, next)
        }
    }

    private suspend fun sendReadReceipts(chatId: String) {
        val incoming = messageDao.getIncomingForChat(chatId, currentUserId)
        for (entity in incoming) {
            mqttManager.sendReceipt(entity.senderId, ReceiptKind.READ, entity.id)
        }
    }

    private suspend fun publishOutgoing(message: Message) {
        try {
            mqttManager.sendMessage(message)
            messageDao.updateStatus(message.id, MessageStatus.SENT)
        } catch (_: Exception) {
            messageDao.updateStatus(message.id, MessageStatus.ERROR)
        }
    }

    private suspend fun retryOutgoing() {
        val pending = messageDao.getOutgoingPending(
            currentUserId,
            listOf(MessageStatus.SENDING, MessageStatus.ERROR)
        )
        for (entity in pending) {
            val message = entity.toModel()
            if (message.status == MessageStatus.ERROR) {
                messageDao.updateStatus(message.id, MessageStatus.SENDING)
            }
            publishOutgoing(message)
        }
    }

    private suspend fun saveMessageAndHandleChat(message: Message) {
        // Always generate the canonical ID from participants to ensure consistency
        val chatId = ChatUtils.getCanonicalChatId(message.senderId, message.receiverId)
        
        // Ensure message is linked to this canonical chat
        messageDao.insertMessage(message.copy(chatId = chatId).toEntity())
        
        // Проверяем существование чата и обновляем его
        val existingChats = allChats.value
        val chat = existingChats.find { it.id == chatId }
        
        val otherUserId = if (message.senderId == currentUserId) message.receiverId else message.senderId
        val isIncoming = message.senderId != currentUserId

        if (chat == null) {
            chatDao.insertChat(
                ChatEntity(
                    id = chatId,
                    name = otherUserId,
                    unreadCount = if (isIncoming) 1 else 0
                )
            )
        } else if (isIncoming) {
            chatDao.updateUnreadCount(chatId, chat.unreadCount + 1)
        }
    }

    // Helper extensions
    private fun Message.toEntity() = MessageEntity(
        id = id,
        chatId = chatId,
        senderId = senderId,
        receiverId = receiverId,
        text = text,
        timestamp = timestamp,
        status = status,
        type = type
    )

    private fun MessageEntity.toModel() = Message(
        id = id,
        chatId = chatId,
        senderId = senderId,
        receiverId = receiverId,
        text = text,
        timestamp = timestamp,
        status = status,
        type = type
    )
}
