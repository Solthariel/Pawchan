package com.solthariel.pawchan.composer

internal data class AttachmentTileState(
    val label: Int,
    val checked: Boolean,
    val enabled: Boolean,
    val action: Boolean = false,
)
