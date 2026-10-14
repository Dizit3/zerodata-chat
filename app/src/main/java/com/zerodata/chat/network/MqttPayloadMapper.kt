package com.zerodata.chat.network

import com.zerodata.chat.model.LobbyPresence
import com.zerodata.chat.model.Message
import kotlinx.serialization.json.Json
import kotlinx.serialization.encodeToString
import kotlinx.serialization.decodeFromString

/**
 * Интерфейс для преобразования различных моделей в JSON и обратно.
 * Соответствует принципу SRP (Single Responsibility Principle).
 */
interface MqttPayloadMapper {
    fun toMessageJson(message: Message): String
    fun fromMessageJson(json: String): Message?
    fun parseInbound(json: String): MqttInbound?
    fun toReceiptJson(kind: ReceiptKind, messageId: String): String

    fun toLobbyJson(presence: LobbyPresence): String
    fun fromLobbyJson(json: String): LobbyPresence?
}

/**
 * Реализация на базе kotlinx.serialization.
 */
class MqttPayloadMapperImpl : MqttPayloadMapper {
    private val jsonObject = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
    }

    override fun toMessageJson(message: Message): String =
        jsonObject.encodeToString(CompactMessageWire.from(message))

    override fun fromMessageJson(json: String): Message? =
        (parseInbound(json) as? MqttInbound.ChatMessage)?.message

    override fun parseInbound(json: String): MqttInbound? {
        if (json.isBlank()) return null
        try {
            val receipt = jsonObject.decodeFromString<CompactReceiptWire>(json).toInbound()
            if (receipt != null) return receipt
        } catch (_: Exception) {
        }
        return try {
            MqttInbound.ChatMessage(
                jsonObject.decodeFromString<CompactMessageWire>(json).toDomain()
            )
        } catch (_: Exception) {
            try {
                MqttInbound.ChatMessage(jsonObject.decodeFromString<Message>(json))
            } catch (_: Exception) {
                null
            }
        }
    }

    override fun toReceiptJson(kind: ReceiptKind, messageId: String): String =
        jsonObject.encodeToString(CompactReceiptWire(kind.wireCode(), messageId))

    override fun toLobbyJson(presence: LobbyPresence): String = jsonObject.encodeToString(presence)

    override fun fromLobbyJson(json: String): LobbyPresence? = try {
        jsonObject.decodeFromString<LobbyPresence>(json)
    } catch (e: Exception) {
        null
    }
}
