package com.zerodata.chat.database

import androidx.room.*
import com.zerodata.chat.model.MessageStatus
import kotlinx.coroutines.flow.Flow

@Dao
interface ChatDao {
    @Transaction
    @Query("SELECT * FROM chats")
    fun getChatsWithLastMessageFlow(): Flow<List<ChatWithLastMessage>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertChat(chat: ChatEntity)

    @Query("UPDATE chats SET unreadCount = :count WHERE id = :chatId")
    suspend fun updateUnreadCount(chatId: String, count: Int)

    @Query("DELETE FROM chats WHERE id = :chatId")
    suspend fun deleteChat(chatId: String)
}

@Dao
interface MessageDao {
    @Query("SELECT * FROM messages WHERE chatId = :chatId ORDER BY timestamp ASC")
    fun getMessagesForChatFlow(chatId: String): Flow<List<MessageEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessage(message: MessageEntity)

    @Query("UPDATE messages SET status = :status WHERE id = :id")
    suspend fun updateStatus(id: String, status: MessageStatus)

    @Query("SELECT * FROM messages WHERE senderId = :userId AND status IN (:statuses) ORDER BY timestamp ASC")
    suspend fun getOutgoingPending(
        userId: String,
        statuses: List<MessageStatus>
    ): List<MessageEntity>

    @Query("SELECT * FROM messages WHERE chatId = :chatId ORDER BY timestamp DESC LIMIT 1")
    suspend fun getLastMessageForChat(chatId: String): MessageEntity?

    @Query("SELECT * FROM messages WHERE id = :id LIMIT 1")
    suspend fun getById(id: String): MessageEntity?

    @Query("SELECT * FROM messages WHERE chatId = :chatId AND senderId != :userId")
    suspend fun getIncomingForChat(chatId: String, userId: String): List<MessageEntity>
}

data class ChatWithLastMessage(
    @Embedded val chat: ChatEntity,
    @Relation(
        parentColumn = "id",
        entityColumn = "chatId",
        entity = MessageEntity::class
    )
    private val allMessages: List<MessageEntity>
) {
    val lastMessage: MessageEntity?
        get() = allMessages.maxByOrNull { it.timestamp }
}
