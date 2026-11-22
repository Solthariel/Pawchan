package com.solthariel.pawchan.composer

import com.solthariel.pawchan.domain.drafts.DraftMarkTool
import com.solthariel.pawchan.domain.drafts.ImageCanvasPoint

internal enum class ImageEditorCanvasMode { FILE, CROP, MARK, ERASE, TEXT }

internal data class ImageEditorCanvasConfig(
    val mode: ImageEditorCanvasMode,
    val cropAspect: Float? = null,
    val markTool: DraftMarkTool? = null,
    val eraser: ImageEraserConfig? = null,
    val textIndex: Int? = null,
    val onTextPoint: (ImageCanvasPoint) -> Unit = {},
    val sourceSize: ReencodingDimensions? = null,
)

internal data class ImageEraserConfig(
    val width: Int,
    val height: Int,
    val radius: Float,
)
