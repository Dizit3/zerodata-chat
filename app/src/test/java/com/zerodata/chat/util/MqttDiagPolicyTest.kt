package com.zerodata.chat.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.net.ConnectException

class MqttDiagPolicyTest {

    @Test
    fun `detects 1883 and skips other ports`() {
        assertTrue(MqttDiagPolicy.isMqtt1883("tcp://10.0.0.1:1883"))
        assertTrue(MqttDiagPolicy.isMqtt1883("tcp://host:1883/"))
        assertFalse(MqttDiagPolicy.isMqtt1883("ssl://host:443"))
        assertFalse(MqttDiagPolicy.isMqtt1883("tcp://host:8883"))
        assertFalse(MqttDiagPolicy.canReport("tcp://h:1883", ""))
        assertTrue(MqttDiagPolicy.canReport("tcp://h:1883", "http://h/diag"))
    }

    @Test
    fun `dedup waits out the interval`() {
        assertTrue(MqttDiagPolicy.shouldSend(null, 1000L, 100L))
        assertFalse(MqttDiagPolicy.shouldSend(1000L, 1050L, 100L))
        assertTrue(MqttDiagPolicy.shouldSend(1000L, 1100L, 100L))
    }

    @Test
    fun `json has no chat text and escapes quotes`() {
        val json = MqttDiagPolicy.toJson(
            kind = "fail",
            error = ConnectException("no \"route\""),
            mqttUri = "tcp://10.0.0.1:1883",
            clientId = "zerodata_me",
            timestampMs = 42L,
        )
        assertTrue(json.contains("\"k\":\"fail\""))
        assertTrue(json.contains("\"c\":\"ConnectException\""))
        assertTrue(json.contains("no \\\"route\\\""))
        assertFalse(json.contains("hello friend"))
        assertEquals("fail:ConnectException", MqttDiagPolicy.eventKey("fail", ConnectException()))
    }
}
