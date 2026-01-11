package com.zerodata.chat.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.zerodata.chat.model.Message

/**
 * Пузырек сообщения в стиле Telegram.
 */
@Composable
fun MessageBubble(message: Message, isMine: Boolean) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 4.dp),
        horizontalAlignment = if (isMine) Alignment.End else Alignment.Start
    ) {
        Box(
            modifier = Modifier
                .background(
                    color = if (isMine) Color(0xFFEFFDDE) else Color.White,
                    shape = RoundedCornerShape(
                        topStart = 12.dp,
                        topEnd = 12.dp,
                        bottomStart = if (isMine) 12.dp else 0.dp,
                        bottomEnd = if (isMine) 0.dp else 12.dp
                    )
                )
                .padding(8.dp)
                .widthIn(max = 280.dp)
        ) {
            Column {
                Text(
                    text = message.text,
                    color = Color.Black,
                    fontSize = 16.sp
                )
                Text(
                    text = formatTime(message.timestamp),
                    color = Color.Gray,
                    fontSize = 11.sp,
                    modifier = Modifier.align(Alignment.End)
                )
            }
        }
    }
}

fun formatTime(timestamp: Long): String {
    // В реальном приложении здесь будет SimpleDateFormat
    return "12:00" 
}
