package com.solthariel.pawchan.security

import com.solthariel.pawchan.composer.ComposerPickerHost

/** Translate composer round-trip notifications; no extra grant, state or lifecycle owner. */
internal class ComposerLockBinding(
    private val selection: AppLockExternalSelection,
) : ComposerPickerHost {
    override fun begin() = selection.begin()

    override fun returned() = selection.returned()

    override fun cancel() = selection.cancel()
}
