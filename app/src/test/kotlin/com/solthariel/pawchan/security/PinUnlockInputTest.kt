package com.solthariel.pawchan.security

import org.junit.Assert.assertEquals
import org.junit.Test

class PinUnlockInputTest {
    @Test fun fourDigitsSubmitOnceAndClearTheSecret() {
        val input = PinUnlockInput()
        val submitted = mutableListOf<String>()
        listOf("1", "12", "123").forEach { input.change(it, true, submitted::add) }
        assertEquals(emptyList<String>(), submitted)
        input.change("1234", true, submitted::add)
        input.change("5678", true, submitted::add)
        assertEquals(listOf("1234"), submitted)
        assertEquals("", input.digits)
    }

    @Test fun deleteAndClearPermitANewAttemptWithoutSavingTheOldPin() {
        val input = PinUnlockInput()
        val submitted = mutableListOf<String>()
        input.change("12", true, submitted::add)
        input.change("1", true, submitted::add)
        assertEquals("1", input.digits)
        input.change("1234", true, submitted::add)
        input.clear()
        input.change("5678", true, submitted::add)
        assertEquals(listOf("1234", "5678"), submitted)
    }

    @Test fun disabledTooLongAndNonAsciiInputNeverSubmit() {
        val input = PinUnlockInput()
        val submitted = mutableListOf<String>()
        input.change("1234", false, submitted::add)
        listOf("12345", "１２３４", "12a4", "-123").forEach { input.change(it, true, submitted::add) }
        assertEquals(emptyList<String>(), submitted)
        assertEquals("", input.digits)
    }
}
