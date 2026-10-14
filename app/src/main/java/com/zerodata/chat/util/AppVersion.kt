package com.zerodata.chat.util

/**
 * Semver for GitHub tags and BuildConfig.VERSION_NAME.
 * Examples: 0.3.0, v0.3.0-pre.1, 0.3.0-rc.2
 * A release without a pre suffix is newer than the same numbers with -pre/-rc.
 */
data class AppVersion(
    val major: Int,
    val minor: Int,
    val patch: Int,
    val preKind: String? = null,
    val preNum: Int = 0
) : Comparable<AppVersion> {

    override fun compareTo(other: AppVersion): Int {
        compareValuesBy(this, other, { it.major }, { it.minor }, { it.patch }).let {
            if (it != 0) return it
        }
        val thisPre = preKind != null
        val otherPre = other.preKind != null
        if (!thisPre && otherPre) return 1
        if (thisPre && !otherPre) return -1
        if (thisPre && otherPre) {
            val kind = (preKind ?: "").compareTo(other.preKind ?: "")
            if (kind != 0) return kind
            return preNum.compareTo(other.preNum)
        }
        return 0
    }

    companion object {
        fun parse(raw: String): AppVersion {
            val cleaned = raw.trim().removePrefix("v").removePrefix("V")
            val coreAndPre = cleaned.split("-", limit = 2)
            val core = coreAndPre[0].split(".")
            val major = core.getOrNull(0)?.toIntOrNull() ?: 0
            val minor = core.getOrNull(1)?.toIntOrNull() ?: 0
            val patch = core.getOrNull(2)?.toIntOrNull() ?: 0
            if (coreAndPre.size == 1) {
                return AppVersion(major, minor, patch)
            }
            val pre = coreAndPre[1].split(".")
            val kind = pre.getOrNull(0)?.lowercase()?.ifBlank { null }
            val num = pre.getOrNull(1)?.toIntOrNull() ?: 0
            return AppVersion(major, minor, patch, kind, num)
        }
    }
}
