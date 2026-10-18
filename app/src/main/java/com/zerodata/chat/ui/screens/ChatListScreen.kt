package com.zerodata.chat.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material.icons.filled.Warning
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.compose.ui.platform.LocalLifecycleOwner
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
import com.zerodata.chat.util.FirstRunOsPrompts
import com.zerodata.chat.util.FirstRunOsPrompts.DataSaverPromptStatus

@Composable
fun ChatListScreen(
    userId: String,
    chats: List<Chat>,
    connectionStatus: Boolean,
    onChatClick: (String) -> Unit,
    onChatDelete: (String) -> Unit = {},
    onAddChatClick: (String) -> Unit,
    onLobbyClick: () -> Unit,
    updateAvailable: GitHubRelease? = null,
    updateProgress: String? = null,
    onUpdateClick: (GitHubRelease) -> Unit = {},
    onDismissUpdate: () -> Unit = {}
) {
    var showAddDialog by remember { mutableStateOf(false) }
    var chatPendingDelete by remember { mutableStateOf<Chat?>(null) }
    var showDataSaverHelp by remember { mutableStateOf(false) }
    
    val clipboardManager = LocalClipboardManager.current
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var dataSaverStatus by remember {
        mutableStateOf(FirstRunOsPrompts.currentDataSaverStatus(context))
    }
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                dataSaverStatus = FirstRunOsPrompts.currentDataSaverStatus(context)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }
    val showDataSaverWarning = dataSaverStatus != null &&
        dataSaverStatus != DataSaverPromptStatus.WHITELISTED


    if (showDataSaverHelp && dataSaverStatus != null) {
        DataSaverHelpDialog(
            status = dataSaverStatus!!,
            onDismiss = { showDataSaverHelp = false },
            onOpenSaver = { FirstRunOsPrompts.openDataSaverSettings(context) },
            onOpenException = { FirstRunOsPrompts.openUnrestrictedDataSettings(context) },
        )
    }

    chatPendingDelete?.let { pending ->
        AlertDialog(
            onDismissRequest = { chatPendingDelete = null },
            title = { Text("Удалить чат?") },
            text = { Text("Чат с ${pending.name} исчезнет только на этом телефоне. Новые сообщения снова создадут диалог.") },
            confirmButton = {
                TextButton(onClick = {
                    onChatDelete(pending.id)
                    chatPendingDelete = null
                }) { Text("Удалить") }
            },
            dismissButton = {
                TextButton(onClick = { chatPendingDelete = null }) { Text("Отмена") }
            },
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
                        if (showDataSaverWarning) {
                            IconButton(onClick = { showDataSaverHelp = true }) {
                                Icon(
                                    Icons.Default.Warning,
                                    contentDescription = "Экономия трафика",
                                    tint = Color(0xFFFFC107)
                                )
                            }
                        }
                        if (updateAvailable != null) {
                            IconButton(onClick = { onUpdateClick(updateAvailable) }) {
                                Icon(
                                    Icons.Default.SystemUpdate,
                                    contentDescription = "Update Available",
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                        IconButton(onClick = onLobbyClick) {
                            Icon(Icons.Default.Public, contentDescription = "Lobby", tint = Color.White)
                        }
                    }
                )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddDialog = true },
                containerColor = MaterialTheme.colorScheme.primary
            ) {
                Icon(Icons.Default.Add, contentDescription = "Новый чат")
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
                ChatListItem(
                    chat = chat,
                    onClick = { onChatClick(chat.id) },
                    onLongClick = { chatPendingDelete = chat },
                )
                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = Color.White.copy(alpha = 0.1f))
            }
        }
    }
}

@Composable
private fun DataSaverHelpDialog(
    status: DataSaverPromptStatus,
    onDismiss: () -> Unit,
    onOpenSaver: () -> Unit,
    onOpenException: () -> Unit,
) {
    val saverOff = status == DataSaverPromptStatus.SAVER_OFF
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Экономия трафика") },
        text = {
            Text(
                if (saverOff) {
                    "Экономия трафика выключена — другие приложения могут съесть 2G. " +
                        "Включите её и разрешите ZeroData без ограничений."
                } else {
                    "Экономия включена, но ZeroData не в исключениях — фон MQTT могут обрезать."
                }
            )
        },
        confirmButton = {
            Column {
                if (saverOff) {
                    TextButton(onClick = onOpenSaver) { Text("Экономия трафика") }
                }
                TextButton(onClick = onOpenException) { Text("Исключение ZeroData") }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Закрыть") }
        }
    )
}
