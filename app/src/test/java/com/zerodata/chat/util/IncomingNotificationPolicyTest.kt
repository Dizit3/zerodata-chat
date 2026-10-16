package com.zerodata.chat.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class IncomingNotificationPolicyTest {

    @Test
    fun `skips own echo and the open chat`() {
        assertFalse(
            IncomingNotificationPolicy.shouldNotify(
                senderId = "me",
                currentUserId = "me",
                chatId = "a_me",
                activeChatId = null,
            ),
        )
        assertFalse(
            IncomingNotificationPolicy.shouldNotify(
                senderId = "friend",
                currentUserId = "me",
                chatId = "friend_me",
                activeChatId = "friend_me",
            ),
        )
        assertTrue(
            IncomingNotificationPolicy.shouldNotify(
                senderId = "friend",
                currentUserId = "me",
                chatId = "friend_me",
                activeChatId = null,
            ),
        )
        assertTrue(
            IncomingNotificationPolicy.shouldNotify(
                senderId = "other",
                currentUserId = "me",
                chatId = "me_other",
                activeChatId = "friend_me",
            ),
        )
    }

    @Test
    fun `stable positive notification ids per chat`() {
        val a = IncomingNotificationPolicy.notificationId("friend_me")
        val b = IncomingNotificationPolicy.notificationId("me_other")
        assertTrue(a >= 0)
        assertTrue(b >= 0)
        assertEquals(a, IncomingNotificationPolicy.notificationId("friend_me"))
        assertNotEquals(a, b)
    }

    @Test
    fun `preview truncates long text`() {
        val long = "x".repeat(100)
        val preview = IncomingNotificationPolicy.previewText(long, 80)
        assertEquals(80, preview.length)
        assertTrue(preview.endsWith("…"))
    }
}
