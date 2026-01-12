package com.zerodata.chat.viewmodel

import androidx.lifecycle.ViewModel
import com.zerodata.chat.model.Chat
import com.zerodata.chat.repository.ChatRepository
import kotlinx.coroutines.flow.StateFlow

class MainViewModel(private val repository: ChatRepository) : ViewModel() {
    
    val chats: StateFlow<List<Chat>> = repository.allChats
    val connectionStatus: StateFlow<Boolean> = repository.connectionStatus

    fun createChat(recipientId: String) {
        repository.createChat(recipientId)
    }
    
    fun clearUnread(chatId: String) {
        repository.clearUnread(chatId)
    }
}
