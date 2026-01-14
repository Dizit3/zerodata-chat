package com.zerodata.chat.viewmodel

import androidx.lifecycle.ViewModel
import com.zerodata.chat.model.Chat
import com.zerodata.chat.repository.ChatRepository
import kotlinx.coroutines.flow.StateFlow

import androidx.lifecycle.viewModelScope
import com.zerodata.chat.network.GitHubRelease
import com.zerodata.chat.util.UpdateManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import android.content.Context

class MainViewModel(
    private val repository: ChatRepository,
    private val context: Context
) : ViewModel() {
    
    val chats: StateFlow<List<Chat>> = repository.allChats
    val connectionStatus: StateFlow<Boolean> = repository.connectionStatus

    private val _updateAvailable = MutableStateFlow<GitHubRelease?>(null)
    val updateAvailable = _updateAvailable.asStateFlow()

    private val _updateProgress = MutableStateFlow<String?>(null)
    val updateProgress = _updateProgress.asStateFlow()

    private val updateManager = UpdateManager(context)

    init {
        checkForUpdates()
    }

    private fun checkForUpdates() {
        viewModelScope.launch {
            // Replace with your actual repo details
            val release = updateManager.checkForUpdates("Dizit3", "zerodata-chat")
            _updateAvailable.value = release
        }
    }

    fun dismissUpdate() {
        _updateAvailable.value = null
    }

    fun downloadAndInstallUpdate(release: GitHubRelease) {
        _updateAvailable.value = null 
        viewModelScope.launch {
            val apkAsset = release.assets.find { it.name.endsWith(".apk") }
            if (apkAsset != null) {
                _updateProgress.value = "Загрузка..."
                val file = updateManager.downloadUpdate(apkAsset.downloadUrl)
                if (file != null) {
                    _updateProgress.value = null
                    updateManager.installUpdate(file)
                } else {
                    _updateProgress.value = "Ошибка загрузки"
                }
            }
        }
    }

    fun createChat(recipientId: String) {
        repository.createChat(recipientId)
    }
    
    fun clearUnread(chatId: String) {
        repository.clearUnread(chatId)
    }
}
