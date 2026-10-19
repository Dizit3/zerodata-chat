package com.zerodata.chat.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.net.ConnectException

class MqttDiagReporterTest {

    @Test
    fun `posts 1883 failure immediately`() {
        val posted = mutableListOf<String>()
        val reporter = reporter(poster = { _, json -> posted.add(json); true })
        reporter.report("fail", ConnectException("timed out"))
        assertEquals(1, posted.size)
        assertTrue(posted[0].contains("timed out"))
    }

    @Test
    fun `skips non-1883 broker`() {
        val posted = mutableListOf<String>()
        val reporter = reporter(
            mqttUri = "ssl://host:443",
            poster = { _, json -> posted.add(json); true },
        )
        reporter.report("fail", ConnectException("x"))
        assertTrue(posted.isEmpty())
    }

    @Test
    fun `queues when HTTP fails then flush sends`() {
        var ok = false
        val posted = mutableListOf<String>()
        val store = InMemoryDiagQueueStore()
        val reporter = reporter(
            store = store,
            poster = { _, json ->
                if (!ok) {
                    false
                } else {
                    posted.add(json)
                    true
                }
            },
        )
        reporter.report("lost", ConnectException("refused"))
        assertTrue(posted.isEmpty())
        assertEquals(1, store.pendingKeys().size)
        ok = true
        reporter.flush()
        assertEquals(1, posted.size)
        assertTrue(store.pendingKeys().isEmpty())
    }

    @Test
    fun `does not spam inside interval after success`() {
        val posted = mutableListOf<String>()
        var now = 1_000L
        val reporter = reporter(
            poster = { _, json -> posted.add(json); true },
            now = { now },
        )
        reporter.report("fail", ConnectException("x"))
        now += 1_000L
        reporter.report("fail", ConnectException("x"))
        assertEquals(1, posted.size)
    }

    private fun reporter(
        mqttUri: String = "tcp://10.0.0.1:1883",
        store: DiagQueueStore = InMemoryDiagQueueStore(),
        poster: (String, String) -> Boolean,
        now: () -> Long = { 1_000L },
    ) = MqttDiagReporterImpl(
        mqttUri = mqttUri,
        ingestUrl = "http://10.0.0.1/diag",
        clientId = "zerodata_me",
        store = store,
        poster = DiagPoster { url, json -> poster(url, json) },
        nowMillis = now,
    )
}
