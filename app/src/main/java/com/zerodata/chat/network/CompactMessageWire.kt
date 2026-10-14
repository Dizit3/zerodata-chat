package com.zerodata.chat.network

import com.zerodata.chat.model.Message
import com.zerodata.chat.util.ChatUtils
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** On-the-wire chat message: short keys, no status/type/chatId. */
@Serializable
internal data class CompactMessageWire(
    @SerialName("i") val id: String,
    @SerialName("f") val senderId: String,
    @SerialName("t") val receiverId: String,
    @SerialName("x") val text: String,
    @SerialName("ts") val timestamp: Long,
) {
    fun toDomain(): Message = Message(
        id = id,
        chatId = ChatUtils.getCanonicalChatId(senderId, receiverId),
        senderId = senderId,
        receiverId = receiverId,
        text = text,
        timestamp = timestamp,
    )

    companion object {
        fun from(message: Message) = CompactMessageWire(
            id = message.id,
            senderId = message.senderId,
            receiverId = message.receiverId,
            text = message.text,
            timestamp = message.timestamp,
        )
    }
}
