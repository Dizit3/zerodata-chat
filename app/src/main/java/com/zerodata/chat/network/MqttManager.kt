package com.zerodata.chat.network

import com.zerodata.chat.model.LobbyPresence
import com.zerodata.chat.model.Message
import kotlinx.coroutines.flow.Flow

/**
 * Интерфейс для управления подключением и перепиской.
 */
interface MqttMessagingManager {
    val connectionStatus: Flow<Boolean>
    suspend fun connect()
    suspend fun disconnect()
    suspend fun sendMessage(message: Message)
    fun observeMessages(): Flow<Message>
}

/**
 * Интерфейс для работы с глобальным лобби и поиском пользователей.
 */
interface MqttDiscoveryManager {
    fun joinLobby()
    fun leaveLobby()
    fun observeLobby(): Flow<LobbyPresence>
}

/**
 * Объединенный интерфейс для реализации.
 */
interface MqttManager : MqttMessagingManager, MqttDiscoveryManager
