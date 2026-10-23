package com.solthariel.pawchan.security

import org.junit.Assert.assertEquals
import org.junit.Test

class ComposerLockBindingTest {
    @Test fun resultAndCancellationAreNotConflated() {
        val events = mutableListOf<String>()
        val picker =
            ComposerLockBinding(
                object : AppLockExternalSelection {
                    override fun begin() {
                        events += "begin"
                    }

                    override fun returned() {
                        events += "returned"
                    }

                    override fun cancel() {
                        events += "cancel"
                    }
                },
            )
        picker.begin()
        picker.returned()
        picker.begin()
        picker.cancel()
        assertEquals(listOf("begin", "returned", "begin", "cancel"), events)
    }
}
