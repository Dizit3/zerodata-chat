package com.zerodata.chat.util

object ChatUtils {
    /**
     * Generates a deterministic Chat ID based on two User IDs.
     * Ensures that (A, B) and (B, A) produce the same ID.
     */
    fun getCanonicalChatId(user1: String, user2: String): String {
        return if (user1 < user2) "${user1}_${user2}" else "${user2}_${user1}"
    }
}
