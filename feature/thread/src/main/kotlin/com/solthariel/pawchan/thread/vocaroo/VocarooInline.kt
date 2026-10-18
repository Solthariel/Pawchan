package com.solthariel.pawchan.thread.vocaroo

import androidx.compose.foundation.text.InlineTextContent
import androidx.compose.foundation.text.appendInlineContent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.Placeholder
import androidx.compose.ui.text.PlaceholderVerticalAlign
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.isSpecified

/**
 * Ссылка Vocaroo остаётся в тексте (копирование, цитаты, поиск видят её как есть), а на экране
 * её место занимает плашка-плеер. Без плеера в композиции показывается обычная ссылка.
 */
internal fun AnnotatedString.Builder.appendVocarooLink(
    url: String,
    text: String,
) {
    pushStringAnnotation(VOCAROO_TAG, url)
    appendInlineContent(VOCAROO_INLINE_PREFIX + url, text)
    pop()
}

internal fun isVocarooLink(url: String): Boolean = vocarooId(url) != null

@Composable
internal fun rememberVocarooInlineContent(
    text: AnnotatedString,
    measurer: TextMeasurer,
    style: TextStyle,
): Map<String, InlineTextContent> {
    val player = LocalVocarooPlayer.current
    val density = LocalDensity.current
    val lineWidth = LocalWindowInfo.current.containerSize.width - with(density) { POST_HORIZONTAL_INSET.roundToPx() }
    return remember(text, player, style, lineWidth) {
        if (player == null) {
            emptyMap()
        } else {
            val fontPx = with(density) { style.fontSize.takeIf { it.isSpecified }?.toPx() } ?: DEFAULT_FONT_PX
            text.getStringAnnotations(VOCAROO_TAG, 0, text.length).associate { link ->
                // Плеер занимает ровно место ссылки: её ширину (но не шире строки) и высоту одной строки,
                // поэтому переносы и расстояния между строками остаются такими же, как с обычной ссылкой.
                val shown = text.substring(link.start, link.end)
                val linkWidth = measurer.measure(shown, style, softWrap = false).size.width
                val width = (linkWidth.coerceIn(0, lineWidth.coerceAtLeast(1)) / fontPx).coerceAtLeast(MIN_WIDTH_EM)
                VOCAROO_INLINE_PREFIX + link.item to
                    InlineTextContent(
                        Placeholder(width.em, CHIP_HEIGHT_EM.em, PlaceholderVerticalAlign.TextCenter),
                    ) { VocarooChip(link.item) }
            }
        }
    }
}

private const val VOCAROO_TAG = "vocaroo"
private const val VOCAROO_INLINE_PREFIX = "vocaroo:"
private const val MIN_WIDTH_EM = 8f

/** Высота строки шрифта без межстрочного зазора: плашка не раздвигает строки. */
private const val CHIP_HEIGHT_EM = 1.15f
private const val DEFAULT_FONT_PX = 40f

/** Поля карточки поста слева и справа: шире строки плашка не бывает. */
private val POST_HORIZONTAL_INSET = 72.dp
