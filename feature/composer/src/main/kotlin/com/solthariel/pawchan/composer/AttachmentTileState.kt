package com.solthariel.pawchan.composer

import com.solthariel.pawchan.feature.composer.R

internal data class AttachmentTileState(
    val label: Int,
    val checked: Boolean,
    val enabled: Boolean,
    val action: Boolean = false,
) {
    val iconOnlySelection: Boolean
        get() =
            label == R.string.attachment_unique_hash ||
                label == R.string.attachment_strip_metadata ||
                label == R.string.markup_spoiler ||
                label == R.string.attachment_rename
}
