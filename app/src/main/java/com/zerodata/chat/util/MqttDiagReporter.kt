package com.zerodata.chat.util

import android.content.Context
import com.zerodata.chat.BuildConfig
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import timber.log.Timber
import java.util.concurrent.TimeUnit

fun interface DiagPoster {
    fun post(url: String, json: String): Boolean
}

interface DiagQueueStore {
    fun lastSentMs(key: String): Long?
    fun markSent(key: String, atMs: Long)
    fun pendingKeys(): List<String>
    fun pendingJson(key: String): String?
    fun putPending(key: String, json: String)
    fun removePending(key: String)
}

interface MqttDiagReporter {
    fun report(kind: String, error: Throwable?)
    fun flush() {}
}

class NoOpMqttDiagReporter : MqttDiagReporter {
    override fun report(kind: String, error: Throwable?) = Unit
}

class InMemoryDiagQueueStore : DiagQueueStore {
    private val lastSent = mutableMapOf<String, Long>()
    private val pending = linkedMapOf<String, String>()

    override fun lastSentMs(key: String): Long? = lastSent[key]
    override fun markSent(key: String, atMs: Long) {
        lastSent[key] = atMs
    }
    override fun pendingKeys(): List<String> = pending.keys.toList()
    override fun pendingJson(key: String): String? = pending[key]
    override fun putPending(key: String, json: String) {
        pending[key] = json
        while (pending.size > MqttDiagPolicy.QUEUE_MAX) {
            pending.remove(pending.keys.first())
        }
    }
    override fun removePending(key: String) {
        pending.remove(key)
    }
}

class SharedPrefsDiagQueueStore(context: Context) : DiagQueueStore {
    private val prefs = context.getSharedPreferences("mqtt_diag", Context.MODE_PRIVATE)

    override fun lastSentMs(key: String): Long? =
        if (prefs.contains("sent:$key")) prefs.getLong("sent:$key", 0L) else null

    override fun markSent(key: String, atMs: Long) {
        prefs.edit().putLong("sent:$key", atMs).apply()
    }

    override fun pendingKeys(): List<String> =
        prefs.getStringSet("pending_keys", emptySet())?.toList() ?: emptyList()

    override fun pendingJson(key: String): String? = prefs.getString("p:$key", null)

    override fun putPending(key: String, json: String) {
        val keys = pendingKeys().toMutableList()
        if (!keys.contains(key)) keys.add(key)
        while (keys.size > MqttDiagPolicy.QUEUE_MAX) {
            val drop = keys.removeAt(0)
            prefs.edit().remove("p:$drop").apply()
        }
        prefs.edit()
            .putString("p:$key", json)
            .putStringSet("pending_keys", keys.toSet())
            .apply()
    }

    override fun removePending(key: String) {
        val keys = pendingKeys().toMutableList()
        keys.remove(key)
        prefs.edit()
            .remove("p:$key")
            .putStringSet("pending_keys", keys.toSet())
            .apply()
    }
}

class OkHttpDiagPoster : DiagPoster {
    private val client = OkHttpClient.Builder()
        .connectTimeout(8, TimeUnit.SECONDS)
        .readTimeout(8, TimeUnit.SECONDS)
        .writeTimeout(8, TimeUnit.SECONDS)
        .build()

    override fun post(url: String, json: String): Boolean {
        return try {
            val body = json.toRequestBody(JSON)
            val request = Request.Builder().url(url).post(body).build()
            client.newCall(request).execute().use { it.isSuccessful }
        } catch (e: Exception) {
            Timber.w(e, "MQTT diag POST failed")
            false
        }
    }

    companion object {
        private val JSON = "application/json; charset=utf-8".toMediaType()
    }
}

class MqttDiagReporterImpl(
    private val mqttUri: String = Constants.MQTT_SERVER_URI,
    private val ingestUrl: String = BuildConfig.DIAG_INGEST_URL,
    private val clientId: String,
    private val store: DiagQueueStore,
    private val poster: DiagPoster,
    private val nowMillis: () -> Long = { System.currentTimeMillis() },
) : MqttDiagReporter {

    override fun report(kind: String, error: Throwable?) {
        if (!MqttDiagPolicy.canReport(mqttUri, ingestUrl)) return
        val key = MqttDiagPolicy.eventKey(kind, error)
        val now = nowMillis()
        if (!MqttDiagPolicy.shouldSend(store.lastSentMs(key), now)) return
        val json = MqttDiagPolicy.toJson(kind, error, mqttUri, clientId, now)
        if (trySend(key, json, now)) return
        store.putPending(key, json)
    }

    override fun flush() {
        if (!MqttDiagPolicy.canReport(mqttUri, ingestUrl)) return
        val now = nowMillis()
        for (key in store.pendingKeys()) {
            val json = store.pendingJson(key) ?: continue
            if (!MqttDiagPolicy.shouldSend(store.lastSentMs(key), now)) continue
            if (!trySend(key, json, now)) break
        }
    }

    private fun trySend(key: String, json: String, now: Long): Boolean {
        if (!poster.post(ingestUrl, json)) return false
        store.markSent(key, now)
        store.removePending(key)
        return true
    }
}
