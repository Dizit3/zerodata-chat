package com.zerodata.chat.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zerodata.chat.model.Message
import com.zerodata.chat.repository.ChatRepository
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class ChatViewModel(
    private val repository: ChatRepository,
    private val chatId: String
) : ViewModel() {
    
    val messages: StateFlow<List<Message>> = repository.getMessages(chatId)

    init {
        repository.setActiveChat(chatId)
    }

    fun sendMessage(text: String) {
        viewModelScope.launch {
            repository.sendMessage(chatId, text)
        }
    }

    override fun onCleared() {
        repository.setActiveChat(null)
        super.onCleared()
    }
}
