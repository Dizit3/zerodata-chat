package com.zerodata.chat.util

object MqttWatchdog {
    fun shouldAttempt(connected: Boolean, elapsedMs: Long, minIntervalMs: Long): Boolean {
        if (connected) return false
        return elapsedMs >= minIntervalMs
    }
}
