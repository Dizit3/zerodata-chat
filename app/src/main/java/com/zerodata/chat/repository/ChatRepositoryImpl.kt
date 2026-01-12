package com.zerodata.chat.repository

import com.zerodata.chat.database.ChatDao
import com.zerodata.chat.database.ChatEntity
import com.zerodata.chat.database.MessageDao
import com.zerodata.chat.database.MessageEntity
import com.zerodata.chat.model.Chat
import com.zerodata.chat.model.Message
import com.zerodata.chat.network.MqttMessagingManager
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
        val message = Message(
            chatId = chatId,
            senderId = currentUserId,
            receiverId = chatId,
            text = text
        )
        
        // Сохраняем локально в БД
        saveMessageAndHandleChat(message, isIncoming = false)
        
        // Отправляем в сеть
        mqttManager.sendMessage(message)
    }

    override fun createChat(recipientId: String) {
        scope.launch {
            chatDao.insertChat(
                ChatEntity(
                    id = recipientId,
                    name = recipientId,
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
        saveMessageAndHandleChat(message, isIncoming = true)
    }

    private suspend fun saveMessageAndHandleChat(message: Message, isIncoming: Boolean) {
        val chatId = message.chatId
        
        // Сохраняем сообщение
        messageDao.insertMessage(message.toEntity())
        
        // Проверяем существование чата и обновляем его
        val existingChats = allChats.value
        val chat = existingChats.find { it.id == chatId }
        
        if (chat == null) {
            chatDao.insertChat(
                ChatEntity(
                    id = chatId,
                    name = message.senderId,
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
