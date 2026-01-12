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

    override fun toMessageJson(message: Message): String = jsonObject.encodeToString(message)

    override fun fromMessageJson(json: String): Message? = try {
        jsonObject.decodeFromString<Message>(json)
    } catch (e: Exception) {
        null
    }

    override fun toLobbyJson(presence: LobbyPresence): String = jsonObject.encodeToString(presence)

    override fun fromLobbyJson(json: String): LobbyPresence? = try {
        jsonObject.decodeFromString<LobbyPresence>(json)
    } catch (e: Exception) {
        null
    }
}
