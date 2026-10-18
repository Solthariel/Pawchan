package com.solthariel.pawchan.workspace

import com.solthariel.pawchan.domain.model.ThreadAddress

internal data class PendingPreviewReply(
    val address: ThreadAddress,
    val text: String,
)
