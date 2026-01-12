package com.zerodata.chat.repository

import com.zerodata.chat.model.Chat
import com.zerodata.chat.model.Message
import com.zerodata.chat.network.MqttManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

/**
 * Реализация репозитория, хранящая данные в оперативной памяти.
 */
class ChatRepositoryImpl(
    private val mqttManager: MqttManager,
    private val currentUserId: String,
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
) : ChatRepository {

    private val _allChats = MutableStateFlow<List<Chat>>(emptyList())
    override val allChats: StateFlow<List<Chat>> = _allChats.asStateFlow()

    private val _messagesByChat = mutableMapOf<String, MutableStateFlow<List<Message>>>()

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
        return _messagesByChat.getOrPut(chatId) {
            MutableStateFlow(emptyList())
        }.asStateFlow()
    }

    override suspend fun sendMessage(chatId: String, text: String) {
        val message = Message(
            chatId = chatId,
            senderId = currentUserId,
            receiverId = chatId,
            text = text
        )
        
        // Сначала добавляем локально для мгновенной реакции UI (Optimistic UI)
        addMessageToFlow(chatId, message)
        updateChatLastMessage(chatId, message, isIncoming = false)
        
        // Отправляем в сеть
        mqttManager.sendMessage(message)
    }

    override fun createChat(recipientId: String) {
        if (_allChats.value.any { it.id == recipientId }) return
        
        val newChat = Chat(
            id = recipientId,
            name = recipientId,
            unreadCount = 0
        )
        _allChats.value = _allChats.value + newChat
    }

    override fun clearUnread(chatId: String) {
        _allChats.value = _allChats.value.map { chat ->
            if (chat.id == chatId) chat.copy(unreadCount = 0) else chat
        }
    }

    private fun handleIncomingMessage(message: Message) {
        val chatId = message.chatId
        addMessageToFlow(chatId, message)
        updateChatLastMessage(chatId, message, isIncoming = true)
    }

    private fun addMessageToFlow(chatId: String, message: Message) {
        val flow = _messagesByChat.getOrPut(chatId) {
            MutableStateFlow(emptyList())
        }
        flow.value = flow.value + message
    }

    private fun updateChatLastMessage(chatId: String, message: Message, isIncoming: Boolean) {
        val currentChats = _allChats.value
        val existingChat = currentChats.find { it.id == chatId }
        
        if (existingChat != null) {
            _allChats.value = currentChats.map { chat ->
                if (chat.id == chatId) {
                    chat.copy(
                        lastMessage = message,
                        unreadCount = if (isIncoming) chat.unreadCount + 1 else chat.unreadCount
                    )
                } else chat
            }
        } else {
            // Если чата нет, создаем его (случай первого входящего сообщения)
            val newChat = Chat(
                id = chatId,
                name = message.senderId,
                lastMessage = message,
                unreadCount = if (isIncoming) 1 else 0
            )
            _allChats.value = currentChats + newChat
        }
    }
}
