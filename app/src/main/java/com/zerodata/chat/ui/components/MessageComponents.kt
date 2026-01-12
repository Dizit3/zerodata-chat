package com.zerodata.chat.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/**
 * Поле ввода сообщения.
 */
@Composable
fun MessageInput(
    onSendMessage: (String) -> Unit
) {
    var textState by remember { mutableStateOf("") }

    BottomAppBar(
        containerColor = Color(0xFF1E1E1E),
        contentPadding = PaddingValues(horizontal = 8.dp)
    ) {
        TextField(
            value = textState,
            onValueChange = { textState = it },
            modifier = Modifier.weight(1f),
            placeholder = { Text("Сообщение...", color = Color.White.copy(alpha = 0.4f)) },
            colors = TextFieldDefaults.colors(
                focusedContainerColor = Color.Transparent,
                unfocusedContainerColor = Color.Transparent,
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White
            )
        )
        IconButton(onClick = {
            if (textState.isNotBlank()) {
                onSendMessage(textState)
                textState = ""
            }
        }) {
            Icon(Icons.Default.Send, contentDescription = "Send", tint = Color(0xFF2196F3))
        }
    }
}
