package com.zerodata.chat.util

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class FirstRunOsPromptsTest {

    @Test
    fun `battery dialog only on first run when not already ignoring`() {
        assertTrue(FirstRunOsPrompts.shouldRequestBatteryExemption(false, false))
        assertFalse(FirstRunOsPrompts.shouldRequestBatteryExemption(true, false))
        assertFalse(FirstRunOsPrompts.shouldRequestBatteryExemption(false, true))
        assertFalse(FirstRunOsPrompts.shouldRequestBatteryExemption(true, true))
    }

    @Test
    fun `data saver prompt when saver off or app restricted, once`() {
        val off = FirstRunOsPrompts.DataSaverPromptStatus.SAVER_OFF
        val restricted = FirstRunOsPrompts.DataSaverPromptStatus.RESTRICTED
        val ok = FirstRunOsPrompts.DataSaverPromptStatus.WHITELISTED
        assertTrue(FirstRunOsPrompts.shouldPromptDataSaver(off, false))
        assertTrue(FirstRunOsPrompts.shouldPromptDataSaver(restricted, false))
        assertFalse(FirstRunOsPrompts.shouldPromptDataSaver(ok, false))
        assertFalse(FirstRunOsPrompts.shouldPromptDataSaver(off, true))
        assertFalse(FirstRunOsPrompts.shouldPromptDataSaver(restricted, true))
    }
}
