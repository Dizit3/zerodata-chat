package com.zerodata.chat

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.viewmodel.compose.viewModel
import com.zerodata.chat.network.RealMqttManager
import com.zerodata.chat.ui.screens.ChatScreen
import com.zerodata.chat.viewmodel.ChatViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.zerodata.chat.util.GlobalExceptionHandler

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Включаем "улавливатель" всех вылетов
        GlobalExceptionHandler.initialize(applicationContext)
        
        // В реальности ID пользователя должен браться из настроек или регистрации
        val userId = "user_" + (1000..9999).random() 
        val mqttManager = RealMqttManager(applicationContext, userId)
        
        setContent {
            val viewModel: ChatViewModel = viewModel(
                factory = object : ViewModelProvider.Factory {
                    override fun <T : ViewModel> create(modelClass: Class<T>): T {
                        return ChatViewModel(mqttManager) as T
                    }
                }
            )
            val messages by viewModel.messages.collectAsState()
            
            ChatScreen(
                userId = userId,
                messages = messages,
                onSendMessage = { text -> 
                    // Для теста: отправляем самому себе или на фиксированный ID
                    viewModel.sendMessage(text) 
                }
            )
        }
    }
}
