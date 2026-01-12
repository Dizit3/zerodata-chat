package com.zerodata.chat.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.zerodata.chat.model.Message
import com.zerodata.chat.ui.components.MessageBubble
import com.zerodata.chat.ui.components.ZeroDataTopBar
import com.zerodata.chat.ui.components.BackButton
import com.zerodata.chat.ui.components.MessageInput

@Composable
fun ChatScreen(
    userId: String,
    messages: List<Message>,
    onSendMessage: (String) -> Unit,
    onBack: () -> Unit
) {
    Scaffold(
        topBar = {
            ZeroDataTopBar(
                title = "Чат",
                subtitle = "ID: $userId",
                navigationIcon = { BackButton(onClick = onBack) }
            )
        },
        bottomBar = {
            MessageInput(onSendMessage = onSendMessage)
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .background(Color(0xFF121212)),
            reverseLayout = true
        ) {
            items(messages.reversed()) { msg ->
                MessageBubble(message = msg, isMine = msg.senderId == userId)
            }
        }
    }
}
