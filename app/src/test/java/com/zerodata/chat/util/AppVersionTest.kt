package com.zerodata.chat.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AppVersionTest {

    @Test
    fun `numeric order ignores v prefix`() {
        assertTrue(AppVersion.parse("v0.3.0") > AppVersion.parse("0.2.0"))
        assertEquals(0, AppVersion.parse("v0.2.0").compareTo(AppVersion.parse("0.2.0")))
    }

    @Test
    fun `pre-release is older than the same numbers without suffix`() {
        assertTrue(AppVersion.parse("0.3.0-pre.1") < AppVersion.parse("0.3.0"))
        assertTrue(AppVersion.parse("0.2.0") < AppVersion.parse("0.3.0-pre.1"))
    }

    @Test
    fun `pre numbers increase`() {
        assertTrue(AppVersion.parse("0.3.0-pre.2") > AppVersion.parse("0.3.0-pre.1"))
    }
}
