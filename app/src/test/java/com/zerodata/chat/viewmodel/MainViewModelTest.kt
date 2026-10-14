package com.zerodata.chat.viewmodel

import android.content.Context
import com.zerodata.chat.model.Chat
import com.zerodata.chat.network.GitHubAsset
import com.zerodata.chat.network.GitHubRelease
import com.zerodata.chat.repository.ChatRepository
import com.zerodata.chat.util.UpdateManager
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class MainViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var repository: ChatRepository
    private lateinit var context: Context
    private lateinit var updateManager: UpdateManager
    private lateinit var viewModel: MainViewModel

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        repository = mockk(relaxed = true)
        context = mockk(relaxed = true)
        updateManager = mockk(relaxed = true)
        every { context.packageName } returns "com.zerodata.chat"
        every { context.externalCacheDir } returns null
        every { repository.allChats } returns MutableStateFlow(emptyList())
        every { repository.connectionStatus } returns MutableStateFlow(false)
        coEvery { updateManager.checkForUpdates(any(), any()) } returns null
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `viewModel observes allChats from repository`() = runTest(testDispatcher) {
        val mockChats = listOf(Chat(id = "chat1", name = "Chat 1"))
        every { repository.allChats } returns MutableStateFlow(mockChats)

        viewModel = MainViewModel(repository, context, updateManager)
        advanceUntilIdle()

        assertEquals(mockChats, viewModel.chats.value)
    }

    @Test
    fun `when createChat is called then delegates to repository`() = runTest(testDispatcher) {
        viewModel = MainViewModel(repository, context, updateManager)

        viewModel.createChat("user123")
        advanceUntilIdle()

        verify { repository.createChat("user123") }
    }

    @Test
    fun `when clearUnread is called then delegates to repository`() = runTest(testDispatcher) {
        viewModel = MainViewModel(repository, context, updateManager)

        viewModel.clearUnread("chat1")
        advanceUntilIdle()

        verify { repository.clearUnread("chat1") }
    }

    @Test
    fun `dismissUpdate and startUpdateFlow toggle flags`() = runTest(testDispatcher) {
        val release = GitHubRelease(tagName = "v0.4.0")
        coEvery { updateManager.checkForUpdates(any(), any()) } returns release

        viewModel = MainViewModel(repository, context, updateManager)
        advanceUntilIdle()
        assertEquals(release, viewModel.updateAvailable.value)

        viewModel.startUpdateFlow()
        assertTrue(viewModel.showUpdateScreen.value)

        viewModel.dismissUpdate()
        assertNull(viewModel.updateAvailable.value)
        assertFalse(viewModel.showUpdateScreen.value)
    }

    @Test
    fun `downloadAndInstallUpdate without apk sets error`() = runTest(testDispatcher) {
        viewModel = MainViewModel(repository, context, updateManager)
        val release = GitHubRelease(
            tagName = "v0.4.0",
            assets = listOf(
                GitHubAsset(
                    downloadUrl = "https://example.com/notes.txt",
                    name = "notes.txt",
                    contentType = "text/plain",
                ),
            ),
        )

        viewModel.downloadAndInstallUpdate(release)
        advanceUntilIdle()

        assertEquals("Ошибка: APK не найден в релизе.", viewModel.updateProgress.value)
    }
}
