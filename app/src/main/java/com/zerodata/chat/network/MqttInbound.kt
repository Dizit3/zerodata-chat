package com.zerodata.chat.network

import com.zerodata.chat.model.Message
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

sealed class MqttInbound {
    data class ChatMessage(val message: Message) : MqttInbound()
    data class Receipt(val kind: ReceiptKind, val messageId: String) : MqttInbound()
}

enum class ReceiptKind {
    DELIVERED,
    READ,
}

@Serializable
internal data class CompactReceiptWire(
    @SerialName("k") val kind: String,
    @SerialName("i") val id: String,
)

internal fun CompactReceiptWire.toInbound(): MqttInbound.Receipt? {
    val mapped = when (kind) {
        "d" -> ReceiptKind.DELIVERED
        "r" -> ReceiptKind.READ
        else -> null
    } ?: return null
    if (id.isBlank()) return null
    return MqttInbound.Receipt(mapped, id)
}

internal fun ReceiptKind.wireCode(): String = when (this) {
    ReceiptKind.DELIVERED -> "d"
    ReceiptKind.READ -> "r"
}
