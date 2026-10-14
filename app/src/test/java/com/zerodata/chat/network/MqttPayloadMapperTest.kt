package com.zerodata.chat.network

import com.zerodata.chat.model.LobbyPresence
import com.zerodata.chat.model.Message
import com.zerodata.chat.model.MessageStatus
import com.zerodata.chat.model.MessageType
import com.zerodata.chat.util.ChatUtils
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class MqttPayloadMapperTest {

    private val mapper = MqttPayloadMapperImpl()

    @Test
    fun `toMessageJson uses compact short keys`() {
        val message = Message(
            id = "msg-1",
            chatId = "@aaa_@bbb",
            senderId = "@aaa",
            receiverId = "@bbb",
            text = "hi",
            timestamp = 1_700_000_000_000L,
            status = MessageStatus.SENT,
            type = MessageType.TEXT,
        )
        val json = mapper.toMessageJson(message)
        assertTrue(json.contains("\"i\":\"msg-1\""))
        assertTrue(json.contains("\"f\":\"@aaa\""))
        assertTrue(json.contains("\"t\":\"@bbb\""))
        assertTrue(json.contains("\"x\":\"hi\""))
        assertTrue(json.contains("\"ts\":1700000000000"))
        assertFalse(json.contains("senderId"))
        assertFalse(json.contains("chatId"))
        assertFalse(json.contains("status"))
    }

    @Test
    fun `compact round-trip restores message fields`() {
        val original = Message(
            id = "msg-2",
            chatId = "ignored-on-wire",
            senderId = "@user1",
            receiverId = "@user2",
            text = "test",
            timestamp = 42L,
        )
        val decoded = mapper.fromMessageJson(mapper.toMessageJson(original))
        assertNotNull(decoded)
        assertEquals("msg-2", decoded!!.id)
        assertEquals("@user1", decoded.senderId)
        assertEquals("@user2", decoded.receiverId)
        assertEquals("test", decoded.text)
        assertEquals(42L, decoded.timestamp)
        assertEquals(
            ChatUtils.getCanonicalChatId("@user1", "@user2"),
            decoded.chatId,
        )
        assertEquals(MessageStatus.SENDING, decoded.status)
        assertEquals(MessageType.TEXT, decoded.type)
    }

    @Test
    fun `fromMessageJson accepts legacy full Message JSON`() {
        val legacy = """
            {
              "id": "legacy-1",
              "chatId": "@a_@b",
              "senderId": "@a",
              "receiverId": "@b",
              "text": "old",
              "timestamp": 100,
              "status": "SENT",
              "type": "TEXT"
            }
        """.trimIndent()
        val decoded = mapper.fromMessageJson(legacy)
        assertNotNull(decoded)
        assertEquals("legacy-1", decoded!!.id)
        assertEquals("@a_@b", decoded.chatId)
        assertEquals("@a", decoded.senderId)
        assertEquals("old", decoded.text)
        assertEquals(MessageStatus.SENT, decoded.status)
    }

    @Test
    fun `fromMessageJson returns null for garbage`() {
        assertNull(mapper.fromMessageJson("not json"))
        assertNull(mapper.fromMessageJson(""))
        assertNull(mapper.fromMessageJson("{}"))
    }

    @Test
    fun `fromMessageJson ignores extra compact keys`() {
        val json = """{"i":"x","f":"@a","t":"@b","x":"hi","ts":1,"extra":true}"""
        val decoded = mapper.fromMessageJson(json)
        assertNotNull(decoded)
        assertEquals("x", decoded!!.id)
        assertEquals("hi", decoded.text)
    }

    @Test
    fun `lobby presence round-trip`() {
        val presence = LobbyPresence(userId = "@peer", timestamp = 99L)
        val decoded = mapper.fromLobbyJson(mapper.toLobbyJson(presence))
        assertNotNull(decoded)
        assertEquals("@peer", decoded!!.userId)
        assertEquals(99L, decoded.timestamp)
    }

    @Test
    fun `fromLobbyJson returns null for garbage`() {
        assertNull(mapper.fromLobbyJson("not json"))
        assertNull(mapper.fromLobbyJson(""))
        assertNull(mapper.fromLobbyJson("{}"))
    }
}
