package com.zerodata.chat.database

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.zerodata.chat.model.MessageStatus
import com.zerodata.chat.model.MessageType

@Entity(tableName = "chats")
data class ChatEntity(
    @PrimaryKey val id: String,
    val name: String,
    val unreadCount: Int,
    val avatarUrl: String? = null
)

@Entity(tableName = "messages")
data class MessageEntity(
    @PrimaryKey val id: String,
    val chatId: String,
    val senderId: String,
    val receiverId: String,
    val text: String,
    val timestamp: Long,
    val status: MessageStatus,
    val type: MessageType
)
