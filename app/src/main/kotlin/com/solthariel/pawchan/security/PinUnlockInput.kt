package com.solthariel.pawchan.security

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/** Ephemeral input claims submission synchronously, before callbacks or recomposition. */
internal class PinUnlockInput {
    var digits by mutableStateOf("")
        private set
    private var submitted = false

    fun change(
        value: String,
        enabled: Boolean,
        submit: (String) -> Unit,
    ) {
        if (!enabled || submitted) return
        if (value.length > PIN_LENGTH || value.any { it !in '0'..'9' }) return
        digits = value
        if (value.length == PIN_LENGTH) {
            submitted = true
            digits = ""
            submit(value)
        }
    }

    fun clear() {
        digits = ""
        submitted = false
    }
}
