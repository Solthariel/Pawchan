package com.solthariel.pawchan.composer

internal data class AttachmentEditorActions(
    val enabled: Boolean,
    val onLimit: () -> Unit,
)
