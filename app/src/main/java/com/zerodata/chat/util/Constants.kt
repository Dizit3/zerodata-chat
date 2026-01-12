package com.zerodata.chat.util

/**
 * Глобальные константы приложения.
 */
object Constants {
    // MQTT Config
    const val MQTT_SERVER_URI = "tcp://broker.emqx.io:1883"
    const val TOPIC_PREFIX = "zerodata/users"
    const val TOPIC_INBOX_SUFFIX = "inbox"
    
    // QOS
    const val QOS_LEAST_ONCE = 1
    
    // Log Tags
    const val TAG_MQTT = "MQTT"
}
