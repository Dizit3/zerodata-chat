package com.zerodata.chat.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zerodata.chat.model.Chat
import com.zerodata.chat.model.Message
import com.zerodata.chat.network.MqttManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class MainViewModel(private val mqttManager: MqttManager) : ViewModel() {
    
    private val _chats = MutableStateFlow<List<Chat>>(emptyList())
    val chats: StateFlow<List<Chat>> = _chats.asStateFlow()

    val connectionStatus = mqttManager.connectionStatus

    init {
        viewModelScope.launch {
            mqttManager.connect()
            mqttManager.observeMessages().collect { message ->
                handleIncomingMessage(message)
            }
        }
    }

    private fun handleIncomingMessage(message: Message) {
        val existingChat = _chats.value.find { it.id == message.chatId }
        
        if (existingChat != null) {
            // Обновляем существующий чат последним сообщением
            _chats.value = _chats.value.map {
                if (it.id == message.chatId) {
                    it.copy(
                        lastMessage = message,
                        unreadCount = it.unreadCount + 1
                    )
                } else it
            }
        } else {
            // Создаем новый чат
            val newChat = Chat(
                id = message.chatId,
                name = message.senderId, // Временно используем ID отправителя как имя
                lastMessage = message,
                unreadCount = 1
            )
            _chats.value = _chats.value + newChat
        }
    }

    fun createChat(recipientId: String) {
        if (_chats.value.any { it.id == recipientId }) return
        
        val newChat = Chat(
            id = recipientId,
            name = recipientId,
            lastMessage = null,
            unreadCount = 0
        )
        _chats.value = _chats.value + newChat
    }
    
    fun clearUnread(chatId: String) {
        _chats.value = _chats.value.map {
            if (it.id == chatId) it.copy(unreadCount = 0) else it
        }
    }
}
