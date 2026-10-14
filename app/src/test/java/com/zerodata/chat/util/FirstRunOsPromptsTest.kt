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
}
