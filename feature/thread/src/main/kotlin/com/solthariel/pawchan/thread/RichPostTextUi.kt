package com.solthariel.pawchan.thread

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.ParagraphStyle
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextIndent
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.sp
import com.solthariel.pawchan.core.ui.DARK_POST_TEXT_COLORS
import com.solthariel.pawchan.core.ui.LIGHT_POST_TEXT_COLORS
import com.solthariel.pawchan.core.ui.PostTextColors
import com.solthariel.pawchan.domain.model.PostDocument
import com.solthariel.pawchan.domain.model.PostLink
import com.solthariel.pawchan.domain.model.PostTextRun
import com.solthariel.pawchan.domain.model.PostTextStyle
import com.solthariel.pawchan.thread.vocaroo.appendVocarooLink
import com.solthariel.pawchan.thread.vocaroo.isVocarooLink

internal class RichPostTextUi private constructor(
    private val runs: List<PostTextRun>,
    val initialText: AnnotatedString,
    private val darkText: AnnotatedString,
    private val authorLight: AnnotatedString?,
    private val authorDark: AnnotatedString?,
    private val replyContext: ReplyReferenceContext? = null,
    private val originalPostId: Long? = null,
) {
    val hasMonkeyReply: Boolean get() = authorLight != null

    fun text(
        colors: PostTextColors,
        hideMonkeyReplies: Boolean = false,
    ): AnnotatedString =
        if (colors == DARK_POST_TEXT_COLORS) {
            if (hideMonkeyReplies) authorDark ?: darkText else darkText
        } else {
            if (hideMonkeyReplies) authorLight ?: initialText else initialText
        }

    fun reveal(
        revealedSpoilers: Set<Int>,
        colors: PostTextColors = LIGHT_POST_TEXT_COLORS,
        hideMonkeyReplies: Boolean = false,
    ): AnnotatedString =
        render(runs, revealedSpoilers, colors, originalPostId, hideMonkeyReplies)
            .highlightReplyReference(replyContext)

    fun withReplyContext(context: ReplyReferenceContext): RichPostTextUi =
        RichPostTextUi(
            runs,
            initialText.highlightReplyReference(context),
            darkText.highlightReplyReference(context),
            authorLight?.highlightReplyReference(context),
            authorDark?.highlightReplyReference(context),
            context,
            originalPostId,
        )

    companion object {
        fun create(
            document: PostDocument,
            originalPostId: Long? = null,
        ): RichPostTextUi {
            val runs = document.runs
            val light = render(runs, emptySet(), LIGHT_POST_TEXT_COLORS, originalPostId)
            val needsDark =
                document.hasMonkeyReply ||
                    runs.any {
                        it.link != null || PostTextStyle.RED in it.styles || PostTextStyle.GREENTEXT in it.styles
                    }
            val dark = if (needsDark) render(runs, emptySet(), DARK_POST_TEXT_COLORS, originalPostId) else light

            fun authorText(colors: PostTextColors): AnnotatedString? =
                if (document.hasMonkeyReply) {
                    render(runs, emptySet(), colors, originalPostId, hideMonkeyReplies = true)
                } else {
                    null
                }
            val authorLight = authorText(LIGHT_POST_TEXT_COLORS)
            val authorDark = authorText(DARK_POST_TEXT_COLORS)
            return RichPostTextUi(runs, light, dark, authorLight, authorDark, originalPostId = originalPostId)
        }
    }
}

internal const val MONKEY_BLOCK_TAG = "dvach-monkey-block"
internal const val MONKEY_HEADING = com.solthariel.pawchan.core.ui.MONKEY_REPLY_LABEL
private val blockParagraph = ParagraphStyle(textIndent = TextIndent(firstLine = 10.sp, restLine = 10.sp))

private fun render(
    runs: List<PostTextRun>,
    revealedSpoilers: Set<Int>,
    colors: PostTextColors,
    originalPostId: Long?,
    hideMonkeyReplies: Boolean = false,
): AnnotatedString {
    val result =
        buildAnnotatedString {
            var index = 0
            while (index < runs.size) {
                val block = runs[index].monkeyBlock
                val start = length
                if (block != null && !hideMonkeyReplies) {
                    withStyle(SpanStyle(color = colors.link, fontWeight = FontWeight.SemiBold)) {
                        append(MONKEY_HEADING)
                    }
                    append('\n')
                }
                do {
                    if (block == null || !hideMonkeyReplies) {
                        appendPreparedRun(runs, index, revealedSpoilers, colors, originalPostId)
                    }
                    index++
                } while (index < runs.size && runs[index].monkeyBlock == block)
                if (block != null && !hideMonkeyReplies) {
                    addStyle(blockParagraph, start, length)
                    addStringAnnotation(MONKEY_BLOCK_TAG, block.toString(), start, length)
                }
            }
        }
    // HTML source indentation after a final hidden div must not leave an empty line in the card.
    return if (hideMonkeyReplies && runs.lastOrNull { it.text.isNotBlank() }?.monkeyBlock != null) {
        result.subSequence(0, result.text.trimEnd().length)
    } else {
        result
    }
}

private fun AnnotatedString.Builder.appendPreparedRun(
    runs: List<PostTextRun>,
    index: Int,
    revealedSpoilers: Set<Int>,
    colors: PostTextColors,
    originalPostId: Long?,
) {
    val run = runs[index]
    val hidden = PostTextStyle.SPOILER in run.styles && index !in revealedSpoilers
    val vocaroo = (run.link as? PostLink.External)?.url?.value?.takeIf { !hidden && isVocarooLink(it) }
    val previous = runs.getOrNull(index - 1)
    val continued =
        vocaroo != null && previous != null &&
            previous.link == run.link && previous.monkeyBlock == run.monkeyBlock
    withStyle(run.toSpanStyle(hidden, colors)) {
        when {
            continued -> Unit
            vocaroo != null -> appendVocaroo(run, vocaroo, runs, index)
            else -> appendRun(run, index, hidden, originalPostId)
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
            .takeWhile { it.link == run.link && it.monkeyBlock == run.monkeyBlock }
            .joinToString("") { it.text }
    pushStringAnnotation(EXTERNAL_TAG, url)
    appendVocarooLink(url, text)
    pop()
}

private fun AnnotatedString.Builder.appendRun(
    run: PostTextRun,
    index: Int,
    hidden: Boolean,
    originalPostId: Long?,
) {
    val annotation = run.annotation(index, hidden)
    if (annotation == null) {
        append(run.text)
    } else {
        pushStringAnnotation(annotation.first, annotation.second)
        append(run.text)
        pop()
        linkSuffixRange(run, hidden, originalPostId)?.let { suffix ->
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

private fun PostTextRun.toSpanStyle(
    hidden: Boolean,
    colors: PostTextColors,
): SpanStyle {
    val decorations =
        buildList {
            if (PostTextStyle.UNDERLINE in styles) add(TextDecoration.Underline)
            if (PostTextStyle.STRIKETHROUGH in styles) add(TextDecoration.LineThrough)
        }
    val linked = link != null && !hidden
    return SpanStyle(
        color = textColor(hidden, linked, colors),
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
    colors: PostTextColors,
): Color =
    when {
        hidden -> SPOILER_COLOR
        linked -> colors.link
        PostTextStyle.RED in styles -> colors.red
        PostTextStyle.GREENTEXT in styles -> colors.quote
        else -> Color.Unspecified
    }

internal const val SPOILER_TAG = "spoiler"
internal const val REFERENCE_TAG = "reference"
internal const val EXTERNAL_TAG = "external"
private val SPOILER_COLOR = Color(SPOILER_COLOR_ARGB)
private const val SPOILER_COLOR_ARGB = 0xFF777777
