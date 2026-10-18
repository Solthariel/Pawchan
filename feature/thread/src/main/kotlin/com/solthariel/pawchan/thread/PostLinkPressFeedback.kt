package com.solthariel.pawchan.thread

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextRange

internal const val LINK_TOUCH_PADDING_DP = 6f
internal const val LINK_PRESS_ALPHA = 0.24f

/** UI-confined owner: changes only the drawing layer, never rebuilds annotated post text. */
internal class PostLinkPressFeedback(
    private val text: AnnotatedString,
    private val paddingPx: Float,
) {
    private var layout: TextLayoutResult? = null
    private var range: TextRange? = null
    var path: Path? by mutableStateOf(null)
        private set

    fun updateLayout(value: TextLayoutResult) {
        layout = value
        path = range?.let { value.getPathForRange(it.start, it.end) }
    }

    fun press(position: Offset) {
        val annotation = offsetAt(position)?.let { postLinkAnnotationAt(text, it) }
        range = annotation?.let { TextRange(it.start, it.end) }
        path = range?.let { layout?.getPathForRange(it.start, it.end) }
    }

    fun release() {
        range = null
        path = null
    }

    fun offsetAt(position: Offset): Int? =
        layout?.let { result ->
            val nearest = result.getOffsetForPosition(position)
            when {
                hitOffset(result, nearest, position, 0f) -> nearest
                hitOffset(result, nearest - 1, position, 0f) -> nearest - 1
                hitOffset(result, nearest, position, paddingPx) -> nearest
                hitOffset(result, nearest - 1, position, paddingPx) -> nearest - 1
                else -> null
            }
        }

    private fun hitOffset(
        result: TextLayoutResult,
        index: Int,
        position: Offset,
        padding: Float,
    ): Boolean =
        index in text.text.indices &&
            (padding == 0f || postTextTargetAt(text, index) != null) &&
            result.getBoundingBox(index).inflate(padding).contains(position)
}

internal fun postLinkAnnotationAt(
    text: AnnotatedString,
    offset: Int,
): AnnotatedString.Range<String>? =
    postTextTargetAt(text, offset)
        ?.takeIf { it.tag == REFERENCE_TAG || it.tag == EXTERNAL_TAG }
        ?.let { AnnotatedString.Range(it.item, it.start, it.end, it.tag) }

internal fun postTextTargetAt(
    text: AnnotatedString,
    offset: Int,
): PostTextTarget? {
    val annotations = text.getStringAnnotations(offset, offset)
    val hidden = annotations.firstOrNull { it.tag == SPOILER_TAG && offset >= it.start && offset < it.end }
    val target =
        hidden ?: annotations.firstOrNull {
            (it.tag == REFERENCE_TAG || it.tag == EXTERNAL_TAG) && offset >= it.start && offset < it.end
        }
    return target?.let { PostTextTarget(it.tag, it.item, it.start, it.end) }
}

internal fun Modifier.postLinkPressHighlight(
    feedback: PostLinkPressFeedback,
    color: Color,
): Modifier =
    drawBehind {
        feedback.path?.let { pressed -> clipRect { drawPath(pressed, color) } }
    }
