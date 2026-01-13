package com.zerodata.chat.repository

import com.zerodata.chat.database.ChatDao
import com.zerodata.chat.database.ChatEntity
import com.zerodata.chat.database.MessageDao
import com.zerodata.chat.database.MessageEntity
import com.zerodata.chat.model.Chat
import com.zerodata.chat.model.Message
import com.zerodata.chat.network.MqttMessagingManager
import com.zerodata.chat.util.ChatUtils
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
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
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

    init {
        // Инициируем подключение
        scope.launch {
            mqttManager.connect()
        }

        // Наблюдаем за подключением
        scope.launch {
            mqttManager.connectionStatus.collect {
                _connectionStatus.value = it
            }
        }

        // Наблюдаем за входящими сообщениями
        scope.launch {
            mqttManager.observeMessages().collect { message ->
                handleIncomingMessage(message)
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
            text = text
        )
        
        // Сохраняем локально в БД
        saveMessageAndHandleChat(message)
        
        // Отправляем в сеть
        mqttManager.sendMessage(message)
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

    override fun clearUnread(chatId: String) {
        scope.launch {
            chatDao.updateUnreadCount(chatId, 0)
        }
    }

    private suspend fun handleIncomingMessage(message: Message) {
        saveMessageAndHandleChat(message)
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
