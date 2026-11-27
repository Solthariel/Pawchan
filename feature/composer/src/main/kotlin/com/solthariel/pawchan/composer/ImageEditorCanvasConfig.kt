package com.solthariel.pawchan.composer

import com.solthariel.pawchan.domain.drafts.DraftMarkTool

internal enum class ImageEditorCanvasMode { FILE, CROP, MARK, ERASE }

internal data class ImageEditorCanvasConfig(
    val mode: ImageEditorCanvasMode,
    val cropAspect: Float? = null,
    val markTool: DraftMarkTool? = null,
    val eraser: ImageEraserConfig? = null,
    val sourceSize: ReencodingDimensions? = null,
)

internal data class ImageEraserConfig(
    val width: Int,
    val height: Int,
    val radius: Float,
)
