package com.zerodata.chat.util

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class FirstRunOsPromptsTest {

    private val off = FirstRunOsPrompts.DataSaverPromptStatus.SAVER_OFF
    private val restricted = FirstRunOsPrompts.DataSaverPromptStatus.RESTRICTED
    private val ok = FirstRunOsPrompts.DataSaverPromptStatus.WHITELISTED

    @Test
    fun `battery dialog only on first run when not already ignoring`() {
        assertTrue(FirstRunOsPrompts.shouldRequestBatteryExemption(false, false))
        assertFalse(FirstRunOsPrompts.shouldRequestBatteryExemption(true, false))
        assertFalse(FirstRunOsPrompts.shouldRequestBatteryExemption(false, true))
        assertFalse(FirstRunOsPrompts.shouldRequestBatteryExemption(true, true))
    }

    @Test
    fun `saver toggle dialog only when saver off and not yet asked`() {
        assertTrue(FirstRunOsPrompts.shouldPromptSaverToggle(off, false))
        assertFalse(FirstRunOsPrompts.shouldPromptSaverToggle(off, true))
        assertFalse(FirstRunOsPrompts.shouldPromptSaverToggle(restricted, false))
        assertFalse(FirstRunOsPrompts.shouldPromptSaverToggle(ok, false))
    }

    @Test
    fun `exception dialog after toggle asked or when already restricted`() {
        assertTrue(FirstRunOsPrompts.shouldPromptException(off, true, false))
        assertTrue(FirstRunOsPrompts.shouldPromptException(restricted, false, false))
        assertFalse(FirstRunOsPrompts.shouldPromptException(off, false, false))
        assertFalse(FirstRunOsPrompts.shouldPromptException(ok, true, false))
        assertFalse(FirstRunOsPrompts.shouldPromptException(restricted, true, true))
    }
}
