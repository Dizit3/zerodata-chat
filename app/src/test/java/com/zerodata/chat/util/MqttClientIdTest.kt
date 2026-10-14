package com.zerodata.chat.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MqttClientIdTest {

    @Test
    fun `clientId is stable for the same userId`() {
        val userId = "@a1b2c3d4"
        assertEquals(Constants.mqttClientId(userId), Constants.mqttClientId(userId))
        assertEquals("zerodata_a1b2c3d4", Constants.mqttClientId(userId))
    }

    @Test
    fun `clientId has no random suffix`() {
        assertFalse(Constants.mqttClientId("me").contains("-"))
        assertEquals("zerodata_me", Constants.mqttClientId("me"))
    }

    @Test
    fun `broker URI comes from BuildConfig`() {
        assertEquals(com.zerodata.chat.BuildConfig.MQTT_SERVER_URI, Constants.MQTT_SERVER_URI)
        assertTrue(
            Constants.MQTT_SERVER_URI.startsWith("tcp://") ||
                Constants.MQTT_SERVER_URI.startsWith("ssl://")
        )
    }
}
