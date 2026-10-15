package com.zerodata.chat.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.zerodata.chat.model.Message
import com.zerodata.chat.model.MessageStatus

/**
 * Пузырек сообщения в стиле Telegram.
 */
@Composable
fun MessageBubble(message: Message, isMine: Boolean) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 4.dp),
        horizontalAlignment = if (isMine) Alignment.End else Alignment.Start
    ) {
        val bubbleColor = if (isMine) Color(0xFF2196F3) else Color(0xFF2C2C2E)
        val textColor = Color.White

        Surface(
            color = bubbleColor,
            shape = RoundedCornerShape(
                topStart = 16.dp,
                topEnd = 16.dp,
                bottomStart = if (isMine) 16.dp else 4.dp,
                bottomEnd = if (isMine) 4.dp else 16.dp
            ),
            shadowElevation = 2.dp
        ) {
            Column(
                modifier = Modifier
                    .padding(horizontal = 12.dp, vertical = 8.dp)
                    .widthIn(max = 260.dp)
            ) {
                Text(
                    text = message.text,
                    color = textColor,
                    fontSize = 15.sp,
                    lineHeight = 20.sp
                )
                
                Row(
                    modifier = Modifier.align(Alignment.End),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Text(
                        text = formatTime(message.timestamp),
                        color = textColor.copy(alpha = 0.5f),
                        fontSize = 10.sp
                    )
                    if (isMine) {
                        DeliveryTicks(status = message.status)
                    }
                }
            }
        }
    }
}

@Composable
private fun DeliveryTicks(status: MessageStatus) {
    val muted = Color.White.copy(alpha = 0.5f)
    val readGreen = Color(0xFF69F0AE)
    val (icon, tint) = when (status) {
        MessageStatus.SENDING -> Icons.Default.Schedule to muted
        MessageStatus.SENT -> Icons.Default.Done to muted
        MessageStatus.DELIVERED -> Icons.Default.DoneAll to muted
        MessageStatus.READ -> Icons.Default.DoneAll to readGreen
        MessageStatus.ERROR -> Icons.Default.Error to Color(0xFFFF8A80)
    }
    Icon(
        imageVector = icon,
        contentDescription = status.name,
        modifier = Modifier.size(14.dp),
        tint = tint
    )
}

fun formatTime(timestamp: Long): String {
    val sdf = java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault())
    return sdf.format(java.util.Date(timestamp))
}
