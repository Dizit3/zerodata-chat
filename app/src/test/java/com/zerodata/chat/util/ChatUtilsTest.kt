package com.zerodata.chat.util

import org.junit.Assert.assertEquals
import org.junit.Test

class ChatUtilsTest {

    @Test
    fun `pair order does not change canonical id`() {
        assertEquals(
            ChatUtils.getCanonicalChatId("a", "b"),
            ChatUtils.getCanonicalChatId("b", "a"),
        )
    }

    @Test
    fun `canonical id is min then max`() {
        assertEquals("aaa_zzz", ChatUtils.getCanonicalChatId("zzz", "aaa"))
        assertEquals("friend_me", ChatUtils.getCanonicalChatId("me", "friend"))
    }

    @Test
    fun `same ids join with underscore`() {
        assertEquals("me_me", ChatUtils.getCanonicalChatId("me", "me"))
    }
}
