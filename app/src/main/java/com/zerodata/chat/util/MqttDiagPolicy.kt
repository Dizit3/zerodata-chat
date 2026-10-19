package com.zerodata.chat.util

object MqttDiagPolicy {
    const val MIN_INTERVAL_MS = 15 * 60 * 1000L
    const val QUEUE_MAX = 20
    const val MESSAGE_MAX = 200

    fun isMqtt1883(uri: String): Boolean {
        val hostPort = uri.substringAfter("://", missingDelimiterValue = uri).substringBefore('/')
        return hostPort.endsWith(":1883")
    }

    fun canReport(mqttUri: String, ingestUrl: String): Boolean =
        ingestUrl.isNotBlank() && isMqtt1883(mqttUri)

    fun shouldSend(lastSentAtMs: Long?, nowMs: Long, intervalMs: Long = MIN_INTERVAL_MS): Boolean {
        if (lastSentAtMs == null) return true
        return nowMs - lastSentAtMs >= intervalMs
    }

    fun eventKey(kind: String, error: Throwable?): String =
        kind + ":" + (error?.javaClass?.simpleName ?: "none")

    fun toJson(
        kind: String,
        error: Throwable?,
        mqttUri: String,
        clientId: String,
        timestampMs: Long,
    ): String {
        val cls = error?.javaClass?.simpleName ?: "none"
        val msg = (error?.message ?: "").replace('\n', ' ').take(MESSAGE_MAX)
        return buildString {
            append('{')
            append("\"k\":\"").append(escape(kind)).append("\",")
            append("\"c\":\"").append(escape(cls)).append("\",")
            append("\"m\":\"").append(escape(msg)).append("\",")
            append("\"u\":\"").append(escape(mqttUri)).append("\",")
            append("\"i\":\"").append(escape(clientId)).append("\",")
            append("\"ts\":").append(timestampMs)
            append('}')
        }
    }

    private fun escape(value: String): String =
        value.replace("\\", "\\\\").replace("\"", "\\\"")
}
