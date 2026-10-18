package com.solthariel.pawchan.infrastructure.thread.export

import android.graphics.Typeface
import android.text.SpannableString
import android.text.Spanned
import android.text.StaticLayout
import android.text.TextPaint
import android.text.style.BackgroundColorSpan
import android.text.style.ForegroundColorSpan
import android.text.style.StrikethroughSpan
import android.text.style.StyleSpan
import android.text.style.UnderlineSpan
import androidx.compose.ui.graphics.isSpecified
import androidx.compose.ui.graphics.toArgb
import com.solthariel.pawchan.domain.model.PostTextStyle
import com.solthariel.pawchan.thread.ThreadExportPost
import com.solthariel.pawchan.thread.ThreadShotStyle

internal fun shotHeader(
    post: ThreadExportPost,
    style: ThreadShotStyle,
): SpannableString {
    val prefix = "${post.index} "
    val source = post.header
    val text = SpannableString(prefix + source.text)
    text.setSpan(ForegroundColorSpan(style.text), 0, prefix.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
    text.setSpan(StyleSpan(Typeface.BOLD), 0, prefix.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
    source.spanStyles.forEach { range ->
        val color = range.item.color
        if (color.isSpecified) {
            text.setSpan(
                ForegroundColorSpan(color.toArgb()),
                prefix.length + range.start,
                prefix.length + range.end,
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE,
            )
        }
    }
    return text
}

internal fun shotBody(
    post: ThreadExportPost,
    style: ThreadShotStyle,
): SpannableString {
    val text = SpannableString(post.document.plainText)
    var offset = 0
    post.document.runs.forEach { run ->
        val end = offset + run.text.length
        applyShotRunStyles(text, offset, end, run.styles)
        if (run.link != null && PostTextStyle.SPOILER !in run.styles) {
            text.setSpan(ForegroundColorSpan(style.accent), offset, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
            text.setSpan(UnderlineSpan(), offset, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        }
        offset = end
    }
    return text
}

private fun applyShotRunStyles(
    text: SpannableString,
    start: Int,
    end: Int,
    styles: Set<PostTextStyle>,
) {
    if (PostTextStyle.BOLD in styles) {
        text.setSpan(StyleSpan(Typeface.BOLD), start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
    }
    if (PostTextStyle.ITALIC in styles) {
        text.setSpan(StyleSpan(Typeface.ITALIC), start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
    }
    if (PostTextStyle.UNDERLINE in styles) {
        text.setSpan(UnderlineSpan(), start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
    }
    if (PostTextStyle.STRIKETHROUGH in styles) {
        text.setSpan(StrikethroughSpan(), start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
    }
    val color = shotRunColor(styles)
    if (color != null) {
        text.setSpan(ForegroundColorSpan(color), start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
    }
    if (PostTextStyle.SPOILER in styles) {
        text.setSpan(BackgroundColorSpan(SHOT_SPOILER_COLOR), start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
    }
}

private fun shotRunColor(styles: Set<PostTextStyle>): Int? =
    when {
        PostTextStyle.SPOILER in styles -> SHOT_SPOILER_COLOR
        PostTextStyle.RED in styles -> SHOT_RED_COLOR
        PostTextStyle.GREENTEXT in styles -> SHOT_QUOTE_COLOR
        else -> null
    }

internal fun shotPaint(
    size: Float,
    color: Int,
    bold: Boolean = false,
): TextPaint =
    TextPaint(TextPaint.ANTI_ALIAS_FLAG).apply {
        this.color = color
        textSize = size
        typeface = Typeface.create("sans-serif", if (bold) Typeface.BOLD else Typeface.NORMAL)
    }

internal fun staticShotLayout(
    text: CharSequence,
    paint: TextPaint,
    width: Int,
    lineHeight: Float = paint.fontSpacing,
): StaticLayout =
    StaticLayout.Builder
        .obtain(text, 0, text.length, paint, width)
        .setLineSpacing((lineHeight - paint.fontSpacing).coerceAtLeast(0f), 1f)
        .setIncludePad(false)
        .build()

private const val SHOT_QUOTE_COLOR = -0xb175c6
private const val SHOT_RED_COLOR = 0xFFE53935.toInt()
private const val SHOT_SPOILER_COLOR = 0xFF777777.toInt()
