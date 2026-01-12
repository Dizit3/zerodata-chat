package com.zerodata.chat.network

import com.zerodata.chat.model.LobbyPresence
import com.zerodata.chat.model.Message
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * Интерфейс для преобразования различных моделей в JSON и обратно.
 * Соответствует принципу SRP (Single Responsibility Principle).
 */
interface MqttPayloadMapper {
    fun toMessageJson(message: Message): String
    fun fromMessageJson(json: String): Message?
    
    fun toLobbyJson(presence: LobbyPresence): String
    fun fromLobbyJson(json: String): LobbyPresence?
}

/**
 * Реализация на базе kotlinx.serialization.
 */
class MqttPayloadMapperImpl : MqttPayloadMapper {
    private val json = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
    }

    override fun toMessageJson(message: Message): String = json.encodeToString(message)

    override fun fromMessageJson(jsonString: String): Message? = try {
        json.decodeFromString<Message>(jsonString)
    } catch (e: Exception) {
        null
    }

    override fun toLobbyJson(presence: LobbyPresence): String = json.encodeToString(presence)

    override fun fromLobbyJson(jsonString: String): LobbyPresence? = try {
        json.decodeFromString<LobbyPresence>(jsonString)
    } catch (e: Exception) {
        null
    }
}
