package com.solthariel.pawchan.thread

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import com.solthariel.pawchan.domain.model.PostDocument
import com.solthariel.pawchan.domain.model.PostLink
import com.solthariel.pawchan.domain.model.PostTextRun
import com.solthariel.pawchan.domain.model.PostTextStyle
import com.solthariel.pawchan.thread.vocaroo.appendVocarooLink
import com.solthariel.pawchan.thread.vocaroo.isVocarooLink

internal class RichPostTextUi private constructor(
    private val runs: List<PostTextRun>,
    val initialText: AnnotatedString,
    private val replyContext: ReplyReferenceContext? = null,
) {
    fun reveal(revealedSpoilers: Set<Int>): AnnotatedString =
        render(runs, revealedSpoilers)
            .highlightReplyReference(replyContext)

    fun withReplyContext(context: ReplyReferenceContext): RichPostTextUi =
        RichPostTextUi(runs, initialText.highlightReplyReference(context), context)

    companion object {
        fun create(document: PostDocument): RichPostTextUi {
            val runs = document.runs.toList()
            return RichPostTextUi(runs, render(runs, emptySet()))
        }
    }
}

private fun render(
    runs: List<PostTextRun>,
    revealedSpoilers: Set<Int>,
): AnnotatedString =
    buildAnnotatedString {
        runs.forEachIndexed { index, run ->
            val spoiler = PostTextStyle.SPOILER in run.styles
            val hidden = spoiler && index !in revealedSpoilers
            val vocaroo = (run.link as? PostLink.External)?.url?.value?.takeIf { !hidden && isVocarooLink(it) }
            // Ссылка, разбитая на несколько кусков текста, даёт одну плашку.
            val continued = vocaroo != null && runs.getOrNull(index - 1)?.link == run.link
            withStyle(run.toSpanStyle(hidden)) {
                when {
                    continued -> Unit
                    vocaroo != null -> appendVocaroo(run, vocaroo, runs, index)
                    else -> appendRun(run, index, hidden)
                }
            }
        }
    }

private fun AnnotatedString.Builder.appendVocaroo(
    run: PostTextRun,
    url: String,
    runs: List<PostTextRun>,
    index: Int,
) {
    val text =
        runs
            .drop(index)
            .takeWhile { it.link == run.link }
            .joinToString("") { it.text }
    pushStringAnnotation(EXTERNAL_TAG, url)
    appendVocarooLink(url, text)
    pop()
}

private fun AnnotatedString.Builder.appendRun(
    run: PostTextRun,
    index: Int,
    hidden: Boolean,
) {
    val annotation = run.annotation(index, hidden)
    if (annotation == null) {
        append(run.text)
    } else {
        pushStringAnnotation(annotation.first, annotation.second)
        append(run.text)
        pop()
        linkSuffixRange(run, hidden)?.let { suffix ->
            val runStart = length - run.text.length
            val start = runStart + suffix.marker.first
            addStringAnnotation(LINK_SUFFIX_TAG, suffix.value, start, runStart + suffix.marker.last + 1)
        }
    }
}

private fun PostTextRun.annotation(
    index: Int,
    hidden: Boolean,
): Pair<String, String>? {
    val target = link
    return when {
        hidden -> SPOILER_TAG to index.toString()
        target is PostLink.Reference -> REFERENCE_TAG to referenceToken(target)
        target is PostLink.External -> EXTERNAL_TAG to target.url.value
        else -> null
    }
}

private fun PostTextRun.toSpanStyle(hidden: Boolean): SpanStyle {
    val decorations =
        buildList {
            if (PostTextStyle.UNDERLINE in styles) add(TextDecoration.Underline)
            if (PostTextStyle.STRIKETHROUGH in styles) add(TextDecoration.LineThrough)
        }
    val linked = link != null && !hidden
    return SpanStyle(
        color = textColor(hidden, linked),
        background = if (hidden) SPOILER_COLOR else Color.Unspecified,
        fontWeight = if (PostTextStyle.BOLD in styles) FontWeight.Bold else null,
        fontStyle = if (PostTextStyle.ITALIC in styles) FontStyle.Italic else null,
        textDecoration =
            if (linked) {
                TextDecoration.Underline
            } else {
                decorations.takeIf(List<TextDecoration>::isNotEmpty)?.let(TextDecoration::combine)
            },
    )
}

private fun PostTextRun.textColor(
    hidden: Boolean,
    linked: Boolean,
): Color =
    when {
        hidden -> SPOILER_COLOR
        linked -> LINK_COLOR
        PostTextStyle.RED in styles -> RED_TEXT_COLOR
        PostTextStyle.GREENTEXT in styles -> GREENTEXT_COLOR
        else -> Color.Unspecified
    }

internal const val SPOILER_TAG = "spoiler"
internal const val REFERENCE_TAG = "reference"
internal const val EXTERNAL_TAG = "external"
internal val LINK_COLOR = Color(LINK_COLOR_ARGB)
private val SPOILER_COLOR = Color(SPOILER_COLOR_ARGB)
private val GREENTEXT_COLOR = Color(GREENTEXT_COLOR_ARGB)
private val RED_TEXT_COLOR = Color(RED_TEXT_COLOR_ARGB)
private const val LINK_COLOR_ARGB = 0xFF5677C8
private const val SPOILER_COLOR_ARGB = 0xFF777777
private const val GREENTEXT_COLOR_ARGB = 0xFF4E8A3A
private const val RED_TEXT_COLOR_ARGB = 0xFFE53935
