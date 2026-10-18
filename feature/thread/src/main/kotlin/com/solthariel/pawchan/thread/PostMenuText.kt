package com.solthariel.pawchan.thread

import com.solthariel.pawchan.domain.model.PostDocument
import com.solthariel.pawchan.domain.model.PostTextStyle

internal fun postQuote(post: ThreadPostUi): String =
    ">>${post.id}\n" +
        post.document.plainText
            .lines()
            .joinToString("\n") { "> $it" } + "\n"

internal fun selectedPostQuote(
    postId: Long,
    selectedText: String,
): String =
    ">>$postId\n" +
        selectedText
            .lines()
            .joinToString("\n") { "> $it" } + "\n"

internal fun PostDocument.toPostingMarkup(): String =
    runs.joinToString("") { run ->
        val tags = run.styles.mapNotNull(POSTING_TAGS::get)
        tags.joinToString("") { "[$it]" } + run.text + tags.asReversed().joinToString("") { "[/$it]" }
    }

private val POSTING_TAGS =
    mapOf(
        PostTextStyle.BOLD to "b",
        PostTextStyle.ITALIC to "i",
        PostTextStyle.UNDERLINE to "u",
        PostTextStyle.STRIKETHROUGH to "s",
        PostTextStyle.SPOILER to "spoiler",
    )
