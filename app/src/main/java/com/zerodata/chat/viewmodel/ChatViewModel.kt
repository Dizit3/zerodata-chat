package com.zerodata.chat.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zerodata.chat.model.Message
import com.zerodata.chat.network.MqttManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

class ChatViewModel(
    private val mqttManager: MqttManager,
    private val chatId: String,
    private val currentUserId: String
) : ViewModel() {
    
    private val _messages = MutableStateFlow<List<Message>>(emptyList())
    val messages: StateFlow<List<Message>> = _messages.asStateFlow()

    init {
        viewModelScope.launch {
            mqttManager.observeMessages().collect { newMessage: Message ->
                if (newMessage.chatId == chatId || newMessage.senderId == chatId) {
                    val currentList = _messages.value.toMutableList()
                    currentList.add(newMessage)
                    _messages.value = currentList
                }
            }
        }
    }

    fun sendMessage(text: String) {
        val newMessage = Message(
            chatId = chatId,
            senderId = currentUserId,
            receiverId = chatId,
            text = text
        )
        val currentList = _messages.value.toMutableList()
        currentList.add(newMessage)
        _messages.value = currentList
        
        viewModelScope.launch {
            mqttManager.sendMessage(newMessage)
        }
    }
}
