package com.zerodata.chat.util

import com.zerodata.chat.network.GitHubRelease

object UpdateSelector {
    fun firstNewerApk(releases: List<GitHubRelease>, current: AppVersion): GitHubRelease? {
        return releases.firstOrNull { candidate ->
            !candidate.draft &&
                candidate.assets.any { it.name.endsWith(".apk", ignoreCase = true) } &&
                AppVersion.parse(candidate.tagName) > current
        }
    }
}
