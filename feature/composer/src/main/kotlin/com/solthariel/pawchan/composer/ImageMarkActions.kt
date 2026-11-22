package com.solthariel.pawchan.composer

import com.solthariel.pawchan.domain.drafts.DraftImageEdit

internal data class ImageMarkActions(
    val onStart: () -> Unit,
    val onChange: (DraftImageEdit) -> Unit,
    val onFinish: () -> Unit,
    val onCancel: () -> Unit,
    val onLimit: () -> Unit,
)

internal fun ImageEditorState.markActions(onLimit: () -> Unit): ImageMarkActions =
    ImageMarkActions(::begin, ::preview, ::finish, ::cancel, onLimit)

internal fun ImageEditorState.cropActions(): ImageCropActions = ImageCropActions(::begin, ::preview, ::finish, ::cancel)
