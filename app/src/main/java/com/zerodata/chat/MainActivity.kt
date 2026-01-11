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

import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.NavType
import androidx.navigation.navArgument
import com.zerodata.chat.ui.screens.ChatListScreen
import com.zerodata.chat.viewmodel.MainViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Включаем "улавливатель" всех вылетов
        GlobalExceptionHandler.initialize(applicationContext)
        
        val userId = "user_" + (1000..9999).random() 
        val mqttManager = RealMqttManager(applicationContext, userId)
        
        setContent {
            val navController = rememberNavController()
            val mainViewModel: MainViewModel = viewModel(
                factory = object : ViewModelProvider.Factory {
                    override fun <T : ViewModel> create(modelClass: Class<T>): T {
                        return MainViewModel(mqttManager) as T
                    }
                }
            )

            NavHost(navController = navController, startDestination = "hub") {
                composable("hub") {
                    val chats by mainViewModel.chats.collectAsState()
                    val connectionStatus by mainViewModel.connectionStatus.collectAsState()
                    
                    ChatListScreen(
                        userId = userId,
                        chats = chats,
                        connectionStatus = connectionStatus,
                        onChatClick = { chatId -> 
                            mainViewModel.clearUnread(chatId)
                            navController.navigate("chat/$chatId") 
                        },
                        onAddChatClick = { recipientId ->
                            mainViewModel.createChat(recipientId)
                            navController.navigate("chat/$recipientId")
                        }
                    )
                }
                
                composable(
                    route = "chat/{chatId}",
                    arguments = listOf(navArgument("chatId") { type = NavType.StringType })
                ) { backStackEntry ->
                    val chatId = backStackEntry.arguments?.getString("chatId") ?: ""
                    val chatViewModel: ChatViewModel = viewModel(
                        key = chatId, // Важно! Разные инстансы для разных чатов
                        factory = object : ViewModelProvider.Factory {
                            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                                return ChatViewModel(mqttManager, chatId, userId) as T
                            }
                        }
                    )
                    
                    val messages by chatViewModel.messages.collectAsState()
                    ChatScreen(
                        userId = userId,
                        messages = messages,
                        onSendMessage = { text -> chatViewModel.sendMessage(text) },
                        onBack = { navController.popBackStack() }
                    )
                }
            }
        }
    }
}
