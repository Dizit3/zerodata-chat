package com.zerodata.chat.network

import com.zerodata.chat.model.Message
import com.zerodata.chat.model.MessageStatus
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Заглушка (Mock) реализации MQTT менеджера для демонстрации.
 */
class MqttManagerMock : MqttManager {
    private val _connectionStatus = MutableStateFlow(false)
    override val connectionStatus = _connectionStatus.asStateFlow()

    private val _incomingMessages = MutableSharedFlow<Message>()

    override suspend fun connect() {
        delay(1000)
        _connectionStatus.value = true
    }

    override suspend fun disconnect() {
        _connectionStatus.value = false
    }

    override suspend fun sendMessage(message: Message) {
        // Симуляция отправки
        println("Sending MQTT message: ${message.text} to topic zerodata/chat/${message.chatId}")
        delay(500)
        // В реальности здесь был бы вызов mqttClient.publish(...)
    }

    override fun observeMessages(): Flow<Message> = _incomingMessages.asSharedFlow()

    override fun joinLobby() {
        println("Joined lobby (mock)")
    }

    override fun leaveLobby() {
        println("Left lobby (mock)")
    }

    override fun observeLobby(): Flow<com.zerodata.chat.model.LobbyPresence> = MutableSharedFlow<com.zerodata.chat.model.LobbyPresence>().asSharedFlow()

    // Метод для симуляции входящего сообщения (для тестов)
    suspend fun simulateIncoming(message: Message) {
        _incomingMessages.emit(message)
    }
}
