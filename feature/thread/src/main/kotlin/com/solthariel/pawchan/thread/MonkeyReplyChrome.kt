package com.solthariel.pawchan.thread

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp

/** The actual text is still one selectable, wrapping paragraph group. Decoration has no gestures. */
@Composable
internal fun Modifier.monkeyReplyChrome(
    text: AnnotatedString,
    accent: Color,
    layout: () -> TextLayoutResult?,
): Modifier {
    val blocks = remember(text) { text.getStringAnnotations(MONKEY_BLOCK_TAG, 0, text.length) }
    if (blocks.isEmpty()) return this
    return fillMaxWidth().drawBehind {
        val result = layout() ?: return@drawBehind
        if (result.lineCount == 0) return@drawBehind
        val visibleEnd = result.getLineEnd(result.lineCount - 1, visibleEnd = true)
        val rail = 2.dp.toPx().coerceAtMost(size.width)
        val railX = if (layoutDirection == LayoutDirection.Ltr) 0f else size.width - rail
        for (block in blocks) {
            var end = minOf(block.end, visibleEnd)
            while (end > block.start && text[end - 1] == '\n') end--
            if (end <= block.start) continue
            val top = result.getLineTop(result.getLineForOffset(block.start)).coerceIn(0f, size.height)
            val bottom = result.getLineBottom(result.getLineForOffset(end - 1)).coerceIn(top, size.height)
            val box = Size(size.width, bottom - top)
            val corner = CornerRadius(4.dp.toPx())
            drawRoundRect(accent.copy(alpha = 0.08f), Offset(0f, top), box, corner)
            drawRoundRect(accent.copy(alpha = 0.28f), Offset(0f, top), box, corner, style = Stroke(1.dp.toPx()))
            drawRect(accent.copy(alpha = 0.85f), Offset(railX, top), Size(rail, bottom - top))
        }
    }
}
