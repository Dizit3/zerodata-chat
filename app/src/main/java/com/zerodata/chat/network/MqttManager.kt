package com.zerodata.chat.network

import com.zerodata.chat.model.Message
import kotlinx.coroutines.flow.Flow

/**
 * Интерфейс для взаимодействия с MQTT брокером.
 */
interface MqttManager {
    /**
     * Статус подключения.
     */
    val connectionStatus: Flow<Boolean>

    /**
     * Подключиться к брокеру.
     */
    suspend fun connect()

    /**
     * Отключиться от брокера.
     */
    suspend fun disconnect()

    /**
     * Отправить сообщение в конкретный чат.
     */
    suspend fun sendMessage(message: Message)

    /**
     * Подписаться на новые сообщения.
     */
    fun observeMessages(): Flow<Message>
}
