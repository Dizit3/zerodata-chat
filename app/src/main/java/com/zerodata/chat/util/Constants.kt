package com.zerodata.chat.util

import com.zerodata.chat.BuildConfig

/**
 * Глобальные константы приложения.
 */
object Constants {
    /**
     * Единственный адрес MQTT-брокера. Клиент читает только это поле.
     *
     * Публичный EMQX видит чужие payload — для приватности подними Mosquitto/EMQX
     * на своей Oracle Cloud VM и задай URI в local.properties (файл не в git):
     *   mqtt.server.uri=tcp://YOUR_ORACLE_PUBLIC_IP:1883
     *   mqtt.server.uri=ssl://YOUR_ORACLE_HOST:8883
     * Экран настроек не нужен. Брокер этим пунктом не ставится.
     * ssl:// — системное хранилище сертификатов (Let's Encrypt и т.п.).
     */
    val MQTT_SERVER_URI: String = BuildConfig.MQTT_SERVER_URI
    const val MQTT_KEEP_ALIVE_SECONDS = 60
    const val MQTT_CONNECTION_TIMEOUT_SECONDS = 30
    const val MQTT_WATCHDOG_INTERVAL_MS = 15_000L
    const val MQTT_RECONNECT_MIN_INTERVAL_MS = 15_000L
    const val TOPIC_PREFIX = "zerodata/users"
    const val TOPIC_INBOX_SUFFIX = "inbox"
    const val TOPIC_LOBBY = "zerodata/public/lobby"

    const val LOBBY_HEARTBEAT_MS = 30_000L
    const val LOBBY_PEER_TTL_MS = 90_000L

    // QOS
    const val QOS_AT_MOST_ONCE = 0
    const val QOS_LEAST_ONCE = 1

    fun mqttClientId(userId: String): String =
        "zerodata_" + userId.filter { it.isLetterOrDigit() || it == '_' }
    
    // Log Tags
    const val TAG_MQTT = "MQTT"
}
