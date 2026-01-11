package com.zerodata.chat.model

import java.util.UUID

/**
 * Модель сообщения в чате
 */
data class Message(
    val id: String = UUID.randomUUID().toString(),
    val chatId: String,
    val senderId: String,
    val receiverId: String = "",
    val text: String,
    val timestamp: Long = System.currentTimeMillis(),
    val status: MessageStatus = MessageStatus.SENDING,
    val type: MessageType = MessageType.TEXT
)

enum class MessageStatus {
    SENDING, SENT, DELIVERED, READ, ERROR
}

enum class MessageType {
    TEXT, IMAGE, SYSTEM
}

/**
 * Модель чата (диалога).
 */
data class Chat(
    val id: String,
    val name: String,
    val lastMessage: Message? = null,
    val unreadCount: Int = 0,
    val avatarUrl: String? = null
)
