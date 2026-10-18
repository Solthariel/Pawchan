package com.solthariel.pawchan.thread

import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.em
import com.solthariel.pawchan.domain.model.PostLink
import com.solthariel.pawchan.domain.model.PostTextRun
import com.solthariel.pawchan.domain.model.ThreadAddress

/**
 * Суффикс ссылки на пост, как LinkSuffixSpan в Dashchan: «OP» — ссылка на ОП-пост,
 * «DT» — ссылка в другой тред. Исходный текст (« (OP)», « →») не меняется: копирование,
 * поиск и экспорт видят его как есть, а на экране маркер рисуется поверх прозрачных символов.
 */
internal const val LINK_SUFFIX_TAG = "reference-link-suffix"

internal data class LinkSuffixRange(
    /** Видимые символы маркера: «(OP)» или «→». */
    val marker: IntRange,
    /** Значение аннотации: [LINK_SUFFIX_DIFFERENT_THREAD] или адрес треда ОП-поста. */
    val value: String,
)

internal fun linkSuffixRange(
    run: PostTextRun,
    hidden: Boolean,
): LinkSuffixRange? {
    val reference = run.link as? PostLink.Reference
    val thread = reference?.thread
    if (hidden || thread == null) return null
    return when {
        run.text.endsWith(OP_SOURCE_SUFFIX) && reference.post.value == thread.thread.value -> {
            LinkSuffixRange((run.text.length - OP_MARKER_LENGTH) until run.text.length, threadToken(thread))
        }

        run.text.endsWith(DIFFERENT_THREAD_SOURCE_SUFFIX) -> {
            LinkSuffixRange((run.text.length - 1) until run.text.length, LINK_SUFFIX_DIFFERENT_THREAD)
        }

        else -> {
            null
        }
    }
}

/** «DT» важнее «OP»: ОП-пост чужого треда помечается как другой тред. */
internal fun linkSuffixLabel(
    value: String,
    context: ThreadAddress?,
): String {
    val differentThread = value == LINK_SUFFIX_DIFFERENT_THREAD || (context != null && value != threadToken(context))
    return if (differentThread) "DT" else "OP"
}

/** Маркер прозрачен; пробел перед ним и сам маркер не подчёркиваются. «→» чуть шире, чтобы вместить «DT». */
internal fun AnnotatedString.withVisualLinkSuffixes(): AnnotatedString =
    AnnotatedString
        .Builder(this)
        .apply {
            this@withVisualLinkSuffixes.getStringAnnotations(LINK_SUFFIX_TAG, 0, length).forEach {
                val spaced = it.start > 0 && this@withVisualLinkSuffixes.text[it.start - 1] == ' '
                val start = if (spaced) it.start - 1 else it.start
                addStyle(SpanStyle(textDecoration = TextDecoration.None), start, it.end)
                addStyle(
                    SpanStyle(
                        color = Color.Transparent,
                        letterSpacing = if (it.item == LINK_SUFFIX_DIFFERENT_THREAD) DT_EXTRA_SPACING.em else 0.em,
                    ),
                    it.start,
                    it.end,
                )
            }
        }.toAnnotatedString()

internal fun Modifier.referenceLinkSuffixes(
    text: AnnotatedString,
    context: ThreadAddress?,
    measurer: TextMeasurer,
    style: TextStyle,
    layout: () -> TextLayoutResult?,
): Modifier =
    drawWithContent {
        drawContent()
        val result = layout() ?: return@drawWithContent
        val suffixStyle = style.merge(TextStyle(color = LINK_COLOR, fontWeight = FontWeight.Medium))
        text.getStringAnnotations(LINK_SUFFIX_TAG, 0, text.length).forEach {
            drawLinkSuffix(result, it.start, linkSuffixLabel(it.item, context), measurer, suffixStyle)
        }
    }

private fun DrawScope.drawLinkSuffix(
    result: TextLayoutResult,
    start: Int,
    label: String,
    measurer: TextMeasurer,
    style: TextStyle,
) {
    if (start >= result.layoutInput.text.length) return
    val line = result.getLineForOffset(start)
    if (line >= result.lineCount || start >= result.getLineEnd(line, visibleEnd = true)) return
    val bounds = result.getBoundingBox(start)
    val measured = measurer.measure(label, style)
    val top = result.getLineBaseline(line) - measured.firstBaseline
    drawText(measured, topLeft = Offset(minOf(bounds.left, bounds.right), top))
}

private fun threadToken(thread: ThreadAddress): String {
    val board = thread.board.board.value
    return "${thread.site.value}|$board|${thread.thread.value}"
}

internal const val LINK_SUFFIX_DIFFERENT_THREAD = "different-thread"
private const val OP_SOURCE_SUFFIX = " (OP)"
private const val DIFFERENT_THREAD_SOURCE_SUFFIX = " →"
private const val OP_MARKER_LENGTH = 4
private const val DT_EXTRA_SPACING = 0.4f
