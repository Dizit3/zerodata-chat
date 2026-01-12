package com.zerodata.chat.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.zerodata.chat.viewmodel.LobbyViewModel
import com.zerodata.chat.ui.components.ZeroDataTopBar
import com.zerodata.chat.ui.components.BackButton
import com.zerodata.chat.ui.components.LobbyUserItem

@Composable
fun LobbyScreen(
    viewModel: LobbyViewModel,
    onUserClick: (String) -> Unit,
    onBack: () -> Unit
) {
    val users by viewModel.users.collectAsState()

    Scaffold(
        topBar = {
            ZeroDataTopBar(
                title = "Глобальное лобби",
                navigationIcon = { BackButton(onClick = onBack) }
            )
        },
        containerColor = Color(0xFF121212)
    ) { padding ->
        if (users.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator(color = Color(0xFF2196F3))
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Поиск пользователей...", color = Color.White.copy(alpha = 0.6f))
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding)
            ) {
                items(users) { userId ->
                    LobbyUserItem(userId = userId, onClick = { onUserClick(userId) })
                    HorizontalDivider(color = Color.White.copy(alpha = 0.1f), modifier = Modifier.padding(horizontal = 16.dp))
                }
            }
        }
    }
}
