package com.zerodata.chat.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zerodata.chat.model.LobbyPresence
import com.zerodata.chat.network.MqttDiscoveryManager
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class LobbyViewModel(private val mqttManager: MqttDiscoveryManager, private val currentUserId: String) : ViewModel() {

    private val _users = MutableStateFlow<Map<String, Long>>(emptyMap())
    val users: StateFlow<List<String>> = _users.map { map ->
        map.filter { it.key != currentUserId && System.currentTimeMillis() - it.value < 15000 }
            .keys.toList()
    }.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    init {
        mqttManager.joinLobby()
        
        viewModelScope.launch {
            mqttManager.observeLobby().collect { presence ->
                _users.update { it + (presence.userId to presence.timestamp) }
            }
        }

        // Очистка неактивных пользователей каждые 10 секунд
        viewModelScope.launch {
            while (true) {
                delay(10000)
                _users.update { currentMap ->
                    currentMap.filter { System.currentTimeMillis() - it.value < 15000 }
                }
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        mqttManager.leaveLobby()
    }
}
