package com.zerodata.chat.network

import com.zerodata.chat.model.Message
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * Интерфейс для преобразования сообщений в JSON и обратно.
 */
interface MessageMapper {
    fun toJson(message: Message): String
    fun fromJson(json: String): Message?
}

/**
 * Реализация MessageMapper на базе kotlinx.serialization.
 */
class MessageMapperImpl : MessageMapper {
    private val json = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
    }

    override fun toJson(message: Message): String {
        return json.encodeToString(message)
    }

    override fun fromJson(jsonString: String): Message? {
        return try {
            json.decodeFromString<Message>(jsonString)
        } catch (e: Exception) {
            null
        }
    }
}
