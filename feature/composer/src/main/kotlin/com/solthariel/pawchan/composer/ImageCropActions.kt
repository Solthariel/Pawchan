package com.solthariel.pawchan.composer

import com.solthariel.pawchan.domain.drafts.DraftImageEdit

internal data class ImageCropActions(
    val onStart: () -> Unit,
    val onChange: (DraftImageEdit) -> Unit,
    val onFinish: () -> Unit,
    val onCancel: () -> Unit = onFinish,
)
