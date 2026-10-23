package com.solthariel.pawchan.security

import org.junit.Assert.assertEquals
import org.junit.Test

class PinUnlockPressTest {
    @Test fun rapidKeysUseCurrentInputWithoutAnInterveningRender() {
        val input = PinUnlockInput()
        val submitted = mutableListOf<String>()
        listOf("1", "2", "3", "4", "5").forEach { input.press(it, true, submitted::add) }
        assertEquals(listOf("1234"), submitted)
        assertEquals("", input.digits)
    }

    @Test fun deletingAndRetypingUseLatestDigitsWithinTheSameFrame() {
        val input = PinUnlockInput()
        val submitted = mutableListOf<String>()
        listOf("1", "2", "⌫", "3", "4", "5").forEach { input.press(it, true, submitted::add) }
        assertEquals(listOf("1345"), submitted)
    }

    @Test fun invalidAndDisabledKeysDoNotModifyOrSubmitInput() {
        val input = PinUnlockInput()
        val submitted = mutableListOf<String>()
        input.press("1", true, submitted::add)
        listOf("", "12", "a", "５", "-", "OK").forEach { input.press(it, true, submitted::add) }
        input.press("⌫", false, submitted::add)
        input.press("2", false, submitted::add)
        assertEquals("1", input.digits)
        assertEquals(emptyList<String>(), submitted)
    }

    @Test fun clearAllowsANewAttemptButDoesNotReuseThePreviousSecret() {
        val input = PinUnlockInput()
        val submitted = mutableListOf<String>()
        listOf("1", "2", "3", "4").forEach { input.press(it, true, submitted::add) }
        input.clear()
        input.press("5", true, submitted::add)
        assertEquals("5", input.digits)
        assertEquals(listOf("1234"), submitted)
    }
}
