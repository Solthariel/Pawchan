package com.solthariel.pawchan.thread

import androidx.compose.ui.text.TextRange

/** Paragraphs are bounded by explicit newlines, never by visual wrapping or word boundaries. */
internal fun paragraphRange(
    text: String,
    offset: Int,
): TextRange {
    if (text.isEmpty()) return TextRange.Zero
    val position = offset.coerceIn(0, text.lastIndex)
    val start = text.lastIndexOf('\n', position - 1) + 1
    val end = text.indexOf('\n', position).takeIf { it >= 0 } ?: text.length
    return TextRange(start, end)
}
