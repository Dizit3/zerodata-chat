package com.zerodata.chat.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zerodata.chat.model.Message
import com.zerodata.chat.network.MqttManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class ChatViewModel(private val mqttManager: MqttManager) : ViewModel() {
    
    private val _messages = MutableStateFlow<List<Message>>(emptyList())
    val messages: StateFlow<List<Message>> = _messages.asStateFlow()

    init {
        viewModelScope.launch {
            mqttManager.connect()
            mqttManager.observeMessages().collect { newMessage ->
                _messages.value = _messages.value + newMessage
            }
        }
    }

    fun sendMessage(text: String) {
        val newMessage = Message(
            chatId = "main_chat",
            senderId = "me",
            receiverId = "test_receiver", // Временно для тестов
            text = text
        )
        _messages.value = _messages.value + newMessage
        
        viewModelScope.launch {
            mqttManager.sendMessage(newMessage)
        }
    }
}
