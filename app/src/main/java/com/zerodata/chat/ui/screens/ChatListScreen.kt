package com.zerodata.chat.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Public
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import android.widget.Toast
import com.zerodata.chat.model.Chat
import com.zerodata.chat.network.GitHubRelease
import com.zerodata.chat.ui.components.ZeroDataTopBar
import com.zerodata.chat.ui.components.ChatListItem
import com.zerodata.chat.ui.components.AddChatDialog

@Composable
fun ChatListScreen(
    userId: String,
    chats: List<Chat>,
    connectionStatus: Boolean,
    onChatClick: (String) -> Unit,
    onChatClick: (String) -> Unit,
    onAddChatClick: (String) -> Unit,
    onLobbyClick: () -> Unit,
    updateAvailable: GitHubRelease? = null,
    onUpdateClick: (GitHubRelease) -> Unit = {}
) {
    var showAddDialog by remember { mutableStateOf(false) }
    
    val clipboardManager = LocalClipboardManager.current
    val context = LocalContext.current

    if (updateAvailable != null) {
        AlertDialog(
            onDismissRequest = {}, // Force update not mandatory, but let's keep it simple
            title = { Text("Доступно обновление") },
            text = { Text("Новая версия ${updateAvailable.tagName} доступна для скачивания.") },
            confirmButton = {
                TextButton(onClick = { onUpdateClick(updateAvailable) }) {
                    Text("Скачать")
                }
            },
            dismissButton = {
                TextButton(onClick = { /* Dismiss logic if needed, currently persistent state in VM */ }) {
                    Text("Позже")
                }
            }
        )
    }

    if (showAddDialog) {
        AddChatDialog(
            onDismiss = { showAddDialog = false },
            onConfirm = { recipientId ->
                onAddChatClick(recipientId)
                showAddDialog = false
            }
        )
    }

    Scaffold(
        topBar = {
            ZeroDataTopBar(
                title = "ZeroData",
                subtitle = "My ID: $userId",
                connectionStatus = connectionStatus,
                onSubtitleClick = {
                    clipboardManager.setText(AnnotatedString(userId))
                    Toast.makeText(context, "ID скопирован", Toast.LENGTH_SHORT).show()
                },
                actions = {
                    IconButton(onClick = onLobbyClick) {
                        Icon(Icons.Default.Public, contentDescription = "Lobby", tint = Color.White)
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddDialog = true },
                containerColor = Color(0xFF2196F3),
                contentColor = Color.White
            ) {
                Icon(Icons.Default.Add, contentDescription = "New Chat")
            }
        },
        containerColor = Color(0xFF121212)
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            items(chats) { chat ->
                ChatListItem(chat = chat, onClick = { onChatClick(chat.id) })
                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = Color.White.copy(alpha = 0.1f))
            }
        }
    }
}
