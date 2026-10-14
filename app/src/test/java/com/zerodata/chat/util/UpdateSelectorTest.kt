package com.zerodata.chat.util

import com.zerodata.chat.network.GitHubAsset
import com.zerodata.chat.network.GitHubRelease
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class UpdateSelectorTest {

    private val current = AppVersion.parse("0.3.0-pre.1")

    @Test
    fun `picks first non-draft with apk newer than current`() {
        val older = release("v0.2.0", apk = true)
        val draft = release("v0.4.0", apk = true, draft = true)
        val noApk = release("v0.4.0", apk = false)
        val newer = release("v0.3.0", apk = true)
        val later = release("v0.5.0", apk = true)

        val picked = UpdateSelector.firstNewerApk(
            listOf(older, draft, noApk, newer, later),
            current,
        )
        assertEquals("v0.3.0", picked?.tagName)
    }

    @Test
    fun `returns null when nothing qualifies`() {
        assertNull(
            UpdateSelector.firstNewerApk(
                listOf(
                    release("v0.2.0", apk = true),
                    release("v0.4.0", apk = true, draft = true),
                    release("v0.4.0", apk = false),
                ),
                current,
            ),
        )
    }

    private fun release(tag: String, apk: Boolean, draft: Boolean = false): GitHubRelease {
        val assets = if (apk) {
            listOf(
                GitHubAsset(
                    downloadUrl = "https://example.com/$tag.apk",
                    name = "ZeroDataChat-$tag.apk",
                    contentType = "application/vnd.android.package-archive",
                ),
            )
        } else {
            listOf(
                GitHubAsset(
                    downloadUrl = "https://example.com/notes.txt",
                    name = "notes.txt",
                    contentType = "text/plain",
                ),
            )
        }
        return GitHubRelease(tagName = tag, assets = assets, draft = draft)
    }
}
