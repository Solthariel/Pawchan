package com.solthariel.pawchan.composer

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.solthariel.pawchan.domain.drafts.DEFAULT_REDACTION_COLOR
import com.solthariel.pawchan.domain.drafts.DraftMarkTool
import com.solthariel.pawchan.domain.drafts.DraftRedactionMode
import com.solthariel.pawchan.domain.drafts.DraftRedactionTool
import com.solthariel.pawchan.domain.drafts.DraftShapeKind
import com.solthariel.pawchan.domain.drafts.DraftTextAlignment
import com.solthariel.pawchan.domain.drafts.DraftTextAppearance
import com.solthariel.pawchan.domain.drafts.DraftTextStyle

internal enum class ImageDrawingTool(
    val label: String,
) {
    PEN("Перо"),
    MARKER("Маркер"),
    ARROW("Стрелка"),
    RECTANGLE("Рамка"),
    OVAL("Овал"),
    ERASER("Ластик"),
}

internal class ImageToolSelection {
    var drawing by mutableStateOf(ImageDrawingTool.PEN)
    var color by mutableIntStateOf(IMAGE_EDITOR_COLORS.first())
    var width by mutableFloatStateOf(DEFAULT_MARK_WIDTH)
    var redactionColor by mutableIntStateOf(DEFAULT_REDACTION_COLOR)
    var redactionMode by mutableStateOf(DraftRedactionMode.FILL)
    var redactionTool by mutableStateOf(DraftRedactionTool.BRUSH)
    var textStyle by mutableStateOf(DraftTextStyle.MEME)
    var textAlignment by mutableStateOf(DraftTextAlignment.CENTER)

    fun textAppearance(): DraftTextAppearance =
        DraftTextAppearance(color = color, size = DEFAULT_TEXT_SIZE, style = textStyle, alignment = textAlignment)

    fun drawingConfig(
        sourceWidth: Int,
        sourceHeight: Int,
    ): ImageEditorCanvasConfig =
        if (drawing == ImageDrawingTool.ERASER) {
            ImageEditorCanvasConfig(
                ImageEditorCanvasMode.ERASE,
                eraser = ImageEraserConfig(sourceWidth, sourceHeight, width / 2f),
            )
        } else {
            ImageEditorCanvasConfig(
                ImageEditorCanvasMode.MARK,
                markTool = drawingMark(),
                sourceSize = ReencodingDimensions(sourceWidth, sourceHeight),
            )
        }

    fun redactionConfig(sourceSize: ReencodingDimensions? = null): ImageEditorCanvasConfig =
        ImageEditorCanvasConfig(
            ImageEditorCanvasMode.MARK,
            markTool = DraftMarkTool.Redact(width, redactionMode, redactionTool, redactionColor),
            sourceSize = sourceSize,
        )

    private fun drawingMark(): DraftMarkTool =
        when (drawing) {
            ImageDrawingTool.PEN -> DraftMarkTool.Pen(color, width)
            ImageDrawingTool.MARKER -> DraftMarkTool.Pen(color, width, marker = true)
            ImageDrawingTool.ARROW -> DraftMarkTool.Shape(color, width, DraftShapeKind.ARROW)
            ImageDrawingTool.RECTANGLE -> DraftMarkTool.Shape(color, width, DraftShapeKind.RECTANGLE)
            ImageDrawingTool.OVAL -> DraftMarkTool.Shape(color, width, DraftShapeKind.OVAL)
            ImageDrawingTool.ERASER -> error("Ластик не создаёт штрихи")
        }
}

internal val IMAGE_EDITOR_COLORS =
    listOf(
        COLOR_WHITE,
        COLOR_BLACK,
        COLOR_RED,
        COLOR_ORANGE,
        COLOR_YELLOW,
        COLOR_GREEN,
        COLOR_BLUE,
        COLOR_PURPLE,
    ).map(Long::toInt)
internal const val DEFAULT_MARK_WIDTH = .02f
private const val DEFAULT_TEXT_SIZE = .08f
private const val COLOR_WHITE = 0xFFFFFFFF
private const val COLOR_BLACK = 0xFF000000
private const val COLOR_RED = 0xFFFF3B30
private const val COLOR_ORANGE = 0xFFFF9500
private const val COLOR_YELLOW = 0xFFFFCC00
private const val COLOR_GREEN = 0xFF34C759
private const val COLOR_BLUE = 0xFF0A84FF
private const val COLOR_PURPLE = 0xFFBF5AF2
