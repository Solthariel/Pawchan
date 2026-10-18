package com.solthariel.pawchan.thread

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import com.solthariel.pawchan.core.ui.PostDisplayPreferences
import com.solthariel.pawchan.core.ui.formatPostTime

internal fun ThreadUiState.Content.exportDocument(
    selected: Set<Long>? = null,
    displayPreferences: PostDisplayPreferences? = null,
): ThreadExportDocument =
    ThreadExportDocument(
        id,
        subject,
        posts.filter { selected == null || it.id in selected }.map { post ->
            ThreadExportPost(
                post.metadata.display.index,
                displayPreferences?.let { post.shotDisplayHeader(it) } ?: post.metadata.display.header,
                post.metadata.display.subject,
                post.document,
                post.attachments.toList(),
            )
        },
    )

/** Screenshot header follows the screen's date, author and marker preferences; HTML remains unchanged. */
internal fun ThreadPostUi.shotDisplayHeader(preferences: PostDisplayPreferences): AnnotatedString {
    val display = metadata.display
    val byline = display.byline
    val date = byline.timestamp?.let { formatPostTime(it, preferences) } ?: byline.date
    return buildAnnotatedString {
        append("#$id")
        date?.let { append(" $it") }
        if (preferences.showStateMarkers) {
            if (display.originalPoster) append(" OP")
            if (display.sage) append(" sage")
        }
        val author = byline.author ?: byline.defaultName?.takeIf { preferences.showDefaultName }
        author?.let {
            append(' ')
            val color = byline.authorColor
            if (color == null) append(it) else withStyle(SpanStyle(color = Color(color))) { append(it) }
        }
        byline.tripcode?.let { append(" $it") }
    }
}
