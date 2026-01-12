package com.zerodata.chat.repository

import com.zerodata.chat.model.Chat
import com.zerodata.chat.model.Message
import kotlinx.coroutines.flow.StateFlow

/**
 * Репозиторий для управления чатами и сообщениями.
 * Центральное место хранения состояния и бизнес-логики приложения.
 */
interface ChatRepository {
    /**
     * Список всех активных чатов.
     */
    val allChats: StateFlow<List<Chat>>

    /**
     * Статус подключения (прокси из MqttManager).
     */
    val connectionStatus: StateFlow<Boolean>

    /**
     * Получить поток сообщений для конкретного чата.
     */
    fun getMessages(chatId: String): StateFlow<List<Message>>

    /**
     * Отправить сообщение.
     */
    suspend fun sendMessage(chatId: String, text: String)

    /**
     * Создать новый чат или вернуть существующий.
     */
    fun createChat(recipientId: String)

    /**
     * Сбросить счетчик непрочитанных для чата.
     */
    fun clearUnread(chatId: String)
}
