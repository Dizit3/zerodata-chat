package com.zerodata.chat.util

/**
 * When to show an inbox heads-up. Android posting stays in IncomingMessageNotifier.
 */
object IncomingNotificationPolicy {

    fun shouldNotify(
        senderId: String,
        currentUserId: String,
        chatId: String,
        activeChatId: String?,
    ): Boolean {
        if (senderId == currentUserId) return false
        if (chatId.isBlank()) return false
        return chatId != activeChatId
    }

    fun notificationId(chatId: String): Int = chatId.hashCode() and 0x7fffffff

    fun previewText(text: String, maxLen: Int = 80): String {
        val trimmed = text.trim()
        if (trimmed.length <= maxLen) return trimmed
        return trimmed.take(maxLen - 1) + "…"
    }
}
