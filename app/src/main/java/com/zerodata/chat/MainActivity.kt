package com.zerodata.chat

import com.zerodata.chat.util.ChatUtils

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.zerodata.chat.ui.screens.ChatScreen
import com.zerodata.chat.viewmodel.ChatViewModel
import com.zerodata.chat.util.FirstRunOsPrompts
import com.zerodata.chat.util.GlobalExceptionHandler
import com.zerodata.chat.util.IncomingMessageNotifier

import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.NavType
import androidx.navigation.navArgument
import com.zerodata.chat.ui.screens.ChatListScreen
import com.zerodata.chat.ui.screens.UpdateScreen
import com.zerodata.chat.viewmodel.MainViewModel
import androidx.compose.foundation.layout.Box
import kotlinx.coroutines.flow.MutableStateFlow

import org.koin.androidx.compose.koinViewModel

import org.koin.core.parameter.parametersOf
import org.koin.android.ext.android.inject

class MainActivity : ComponentActivity() {
    
    // Inject the session userId
    private val userId: String by inject()
    private val pendingOpenChatId = MutableStateFlow<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Включаем "улавливатель" всех вылетов
        GlobalExceptionHandler.initialize(applicationContext)
        FirstRunOsPrompts.requestIfNeeded(this)
        pendingOpenChatId.value = intent.chatIdExtra()
        
        setContent {

                val navController = rememberNavController()
                val mainViewModel: MainViewModel = koinViewModel()
                val openChatId by pendingOpenChatId.collectAsState()
                LaunchedEffect(openChatId) {
                    val chatId = openChatId ?: return@LaunchedEffect
                    navController.navigate("chat/$chatId") {
                        launchSingleTop = true
                    }
                    pendingOpenChatId.value = null
                }

                NavHost(navController = navController, startDestination = "hub") {
                    composable("hub") {
                        val chats by mainViewModel.chats.collectAsState(initial = emptyList())
                        val connectionStatus by mainViewModel.connectionStatus.collectAsState(initial = false)
                        val updateAvailable by mainViewModel.updateAvailable.collectAsState()
                        val updateProgress by mainViewModel.updateProgress.collectAsState()
                        val showUpdateScreen by mainViewModel.showUpdateScreen.collectAsState()
                        
                        Box {
                            ChatListScreen(
                                userId = userId,
                                chats = chats,
                                connectionStatus = connectionStatus,
                                onChatClick = { chatId -> 
                                    mainViewModel.clearUnread(chatId)
                                    navController.navigate("chat/$chatId") 
                                },
                                onChatDelete = { chatId ->
                                    mainViewModel.deleteChat(chatId)
                                },
                                onAddChatClick = { recipientId ->
                                    mainViewModel.createChat(recipientId)
                                    val chatId = ChatUtils.getCanonicalChatId(userId, recipientId)
                                    navController.navigate("chat/$chatId")
                                },
                                onLobbyClick = {
                                    navController.navigate("lobby")
                                },
                                updateAvailable = updateAvailable,
                                updateProgress = updateProgress,
                                onUpdateClick = {
                                    mainViewModel.startUpdateFlow()
                                },
                                onDismissUpdate = {
                                    mainViewModel.dismissUpdate()
                                }
                            )

                            if (showUpdateScreen && updateAvailable != null) {
                                UpdateScreen(
                                    release = updateAvailable!!,
                                    progress = updateProgress,
                                    onDownloadClick = { mainViewModel.downloadAndInstallUpdate(updateAvailable!!) },
                                    onBackClick = { mainViewModel.dismissUpdate() }
                                )
                            }
                        }
                    }
                    
                    composable("lobby") {
                        val lobbyViewModel: com.zerodata.chat.viewmodel.LobbyViewModel = koinViewModel()
                        com.zerodata.chat.ui.screens.LobbyScreen(
                            viewModel = lobbyViewModel,
                            onUserClick = { recipientId ->
                                mainViewModel.createChat(recipientId)
                                val chatId = ChatUtils.getCanonicalChatId(userId, recipientId)
                                navController.navigate("chat/$chatId")
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

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        pendingOpenChatId.value = intent.chatIdExtra()
    }

    override fun onResume() {
        super.onResume()
        FirstRunOsPrompts.requestDataSaverStepsIfNeeded(this)
    }

    private fun Intent.chatIdExtra(): String? =
        getStringExtra(IncomingMessageNotifier.EXTRA_CHAT_ID)?.takeIf { it.isNotBlank() }
}
