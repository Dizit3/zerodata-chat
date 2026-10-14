package com.zerodata.chat.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStore
import com.zerodata.chat.model.LobbyPresence
import com.zerodata.chat.network.MqttDiscoveryManager
import com.zerodata.chat.util.Constants
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class LobbyViewModelTest {

    private val dispatcher = StandardTestDispatcher()
    private val mqtt: MqttDiscoveryManager = mockk(relaxed = true)
    private val lobbyFlow = MutableSharedFlow<LobbyPresence>(extraBufferCapacity = 8)
    private var now = 1_000_000L
    private val store = ViewModelStore()

    @Before
    fun setup() {
        Dispatchers.setMain(dispatcher)
        every { mqtt.observeLobby() } returns lobbyFlow
        now = 1_000_000L
    }

    @After
    fun tearDown() {
        store.clear()
        Dispatchers.resetMain()
    }

    @Test
    fun `joinLobby only while ViewModel is alive, leave on clear`() = runTest(dispatcher) {
        createViewModel()
        runCurrent()
        verify { mqtt.joinLobby() }

        store.clear()
        verify { mqtt.leaveLobby() }
    }

    @Test
    fun `peer presence appears in users`() = runTest(dispatcher) {
        val viewModel = createViewModel()
        backgroundScope.launch { viewModel.users.collect { } }
        runCurrent()

        lobbyFlow.emit(LobbyPresence(userId = "peer", timestamp = now))
        runCurrent()

        assertEquals(listOf("peer"), viewModel.users.value)
        store.clear()
    }

    @Test
    fun `own userId is filtered out`() = runTest(dispatcher) {
        val viewModel = createViewModel()
        backgroundScope.launch { viewModel.users.collect { } }
        runCurrent()

        lobbyFlow.emit(LobbyPresence(userId = "me", timestamp = now))
        runCurrent()

        assertFalse(viewModel.users.value.contains("me"))
        assertTrue(viewModel.users.value.isEmpty())
        store.clear()
    }

    @Test
    fun `peer expires after TTL on heartbeat`() = runTest(dispatcher) {
        val viewModel = createViewModel()
        backgroundScope.launch { viewModel.users.collect { } }
        runCurrent()

        lobbyFlow.emit(LobbyPresence(userId = "peer", timestamp = now))
        runCurrent()
        assertEquals(listOf("peer"), viewModel.users.value)

        now += Constants.LOBBY_PEER_TTL_MS
        advanceTimeBy(Constants.LOBBY_HEARTBEAT_MS)
        runCurrent()

        assertTrue(viewModel.users.value.isEmpty())
        store.clear()
    }

    private fun createViewModel(): LobbyViewModel {
        val factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return LobbyViewModel(mqtt, "me") { now } as T
            }
        }
        return ViewModelProvider(store, factory)[LobbyViewModel::class.java]
    }
}
