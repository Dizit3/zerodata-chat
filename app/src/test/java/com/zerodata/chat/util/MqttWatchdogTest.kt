package com.zerodata.chat.util

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MqttWatchdogTest {

    @Test
    fun `no attempt while connected`() {
        assertFalse(MqttWatchdog.shouldAttempt(connected = true, elapsedMs = 60_000, minIntervalMs = 15_000))
    }

    @Test
    fun `no attempt before min interval`() {
        assertFalse(MqttWatchdog.shouldAttempt(connected = false, elapsedMs = 5_000, minIntervalMs = 15_000))
    }

    @Test
    fun `attempt after min interval when disconnected`() {
        assertTrue(MqttWatchdog.shouldAttempt(connected = false, elapsedMs = 15_000, minIntervalMs = 15_000))
    }
}
