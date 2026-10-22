package com.solthariel.pawchan.composer

import androidx.compose.runtime.staticCompositionLocalOf

/** Narrow app-host lifecycle port; no lock state, app classes or repositories enter the feature. */
interface ComposerPickerHost {
    fun begin()

    fun returned()

    fun cancel()
}

/** Stateless default for standalone hosts without an app lock. App supplies its retained owner. */
val LocalComposerPickerHost =
    staticCompositionLocalOf<ComposerPickerHost> {
        object : ComposerPickerHost {
            override fun begin() = Unit

            override fun returned() = Unit

            override fun cancel() = Unit
        }
    }
