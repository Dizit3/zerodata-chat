package com.zerodata.chat.model

import kotlinx.serialization.Serializable
import java.util.UUID

/**
 * Модель сообщения в чате
 */
@Serializable
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

@Serializable
enum class MessageStatus {
    SENDING, SENT, DELIVERED, READ, ERROR
}

fun MessageStatus.deliveryRank(): Int = when (this) {
    MessageStatus.ERROR -> -1
    MessageStatus.SENDING -> 0
    MessageStatus.SENT -> 1
    MessageStatus.DELIVERED -> 2
    MessageStatus.READ -> 3
}

@Serializable
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

/**
 * Модель присутствия в глобальном лобби.
 */
@Serializable
data class LobbyPresence(
    val userId: String,
    val timestamp: Long = System.currentTimeMillis()
)
