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

import org.koin.androidx.compose.koinViewModel

import org.koin.core.parameter.parametersOf
import org.koin.android.ext.android.inject

class MainActivity : ComponentActivity() {
    
    // Inject the session userId
    private val userId: String by inject()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Включаем "улавливатель" всех вылетов
        GlobalExceptionHandler.initialize(applicationContext)
        
        setContent {

                val navController = rememberNavController()
                val mainViewModel: MainViewModel = koinViewModel()

                NavHost(navController = navController, startDestination = "hub") {
                    composable("hub") {
                        val chats by mainViewModel.chats.collectAsState(initial = emptyList())
                        val connectionStatus by mainViewModel.connectionStatus.collectAsState(initial = false)
                        
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
                            },
                            onLobbyClick = {
                                navController.navigate("lobby")
                            }
                        )
                    }
                    
                    composable("lobby") {
                        val lobbyViewModel: com.zerodata.chat.viewmodel.LobbyViewModel = koinViewModel()
                        com.zerodata.chat.ui.screens.LobbyScreen(
                            viewModel = lobbyViewModel,
                            onUserClick = { userId ->
                                mainViewModel.createChat(userId)
                                navController.navigate("chat/$userId")
                            },
                            onBack = { navController.popBackStack() }
                        )
                    }
                    
                    composable(
                        route = "chat/{chatId}",
                        arguments = listOf(navArgument("chatId") { type = NavType.StringType })
                    ) { backStackEntry ->
                        val chatId = backStackEntry.arguments?.getString("chatId") ?: ""
                        val chatViewModel: ChatViewModel = koinViewModel { parametersOf(chatId) }
                        
                        val messages by chatViewModel.messages.collectAsState(initial = emptyList())
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
