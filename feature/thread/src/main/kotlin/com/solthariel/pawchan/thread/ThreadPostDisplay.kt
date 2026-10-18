package com.solthariel.pawchan.thread

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import com.solthariel.pawchan.core.ui.PostStateMarker
import com.solthariel.pawchan.core.ui.visiblePostSubject
import com.solthariel.pawchan.domain.model.BoardPost
import com.solthariel.pawchan.domain.model.PostBadge
import com.solthariel.pawchan.domain.model.ThreadFlags

internal data class ThreadPostDisplay(
    val index: Int = 0,
    val header: AnnotatedString = AnnotatedString(""),
    val badges: List<PostBadge> = emptyList(),
    val subject: String? = null,
    /** Значки состояния в порядке Dashchan: OP, sage, email, закреплён, закрыт, цикличный, бан. */
    val markers: List<PostStateMarker> = emptyList(),
    val byline: ThreadPostByline = ThreadPostByline(),
    /** Тег треда у ОП-поста, без слэшей. */
    val tag: String? = null,
) {
    val originalPoster: Boolean
        get() = PostStateMarker.ORIGINAL_POSTER in markers
    val sage: Boolean
        get() = PostStateMarker.SAGE in markers
}

/**
 * Видимая часть заголовка. Имя по умолчанию («Аноним») хранится отдельно: показывать его
 * или нет, решает глобальная настройка. Время: timestamp сайта, иначе строка даты сайта.
 */
internal data class ThreadPostByline(
    val author: String? = null,
    val authorColor: Int? = null,
    val date: String? = null,
    val timestamp: Long? = null,
    val tripcode: String? = null,
    val email: String? = null,
    val defaultName: String? = null,
)

internal data class ThreadPostChanges(
    val own: Boolean = false,
    val replyToMe: Boolean = false,
    val new: Boolean = false,
    val edited: Boolean = false,
    val deleted: Boolean = false,
    val originalPoster: Boolean = false,
)

/** Сведения треда, которые видны только у ОП-поста. */
internal data class ThreadOpeningInfo(
    val flags: ThreadFlags = ThreadFlags(pinned = false, closed = false, archived = false),
    val tag: String? = null,
)

internal fun preparePostDisplay(
    post: BoardPost,
    index: Int,
    opening: ThreadOpeningInfo? = null,
): ThreadPostDisplay {
    val metadata = post.content.metadata
    val details = post.content.details
    val opener = post.address.post.value == post.address.thread.thread.value
    val op = metadata.originalPoster || opener
    // 2ch подставляет в тему начало текста, если автор её не написал: такую тему не повторяем (как Dashchan).
    val subject = visiblePostSubject(metadata.subject, post.content.document.plainText)
    val author = metadata.name?.takeUnless(::isDefaultAnonymousName)
    val flags = opening?.flags?.takeIf { opener }
    val markers =
        listOfNotNull(
            PostStateMarker.ORIGINAL_POSTER.takeIf { op },
            PostStateMarker.SAGE.takeIf { metadata.sage },
            PostStateMarker.EMAIL.takeIf { details.email != null },
            PostStateMarker.STICKY.takeIf { flags?.pinned == true },
            PostStateMarker.CLOSED.takeIf { flags?.closed == true },
            PostStateMarker.CYCLICAL.takeIf { flags?.endless == true },
            PostStateMarker.BANNED.takeIf { details.banned },
        )
    return ThreadPostDisplay(
        index = index,
        header = postHeader(post, op, author),
        badges = metadata.badges,
        subject = subject,
        markers = markers,
        byline =
            ThreadPostByline(
                author = author,
                authorColor = metadata.nameColor,
                date = compactPostDate(metadata.date),
                timestamp = details.timestamp,
                tripcode = details.tripcode,
                email = details.email,
                defaultName = metadata.name?.takeIf(::isDefaultAnonymousName),
            ),
        tag = opening?.tag?.takeIf { opener },
    )
}

/** Текстовый заголовок для копирования, экспорта и поиска: маркеры OP/sage остаются словами. */
private fun postHeader(
    post: BoardPost,
    op: Boolean,
    author: String?,
): AnnotatedString {
    val metadata = post.content.metadata
    return buildAnnotatedString {
        append("#${post.address.post.value}")
        if (author != null) {
            append(' ')
            val color = metadata.nameColor
            if (color == null) append(author) else withStyle(SpanStyle(color = Color(color))) { append(author) }
        }
        if (op) append(" OP")
        if (metadata.sage) append(" sage")
        compactPostDate(metadata.date)?.let { append(" $it") }
    }
}

private fun isDefaultAnonymousName(name: String): Boolean =
    name.trim().equals("Аноним", ignoreCase = true) ||
        name.trim().equals("Anonymous", ignoreCase = true)

internal fun compactPostDate(date: String?): String? {
    val match = date?.let(POST_DATE::find) ?: return date
    return "${match.groupValues[1]}.${match.groupValues[DATE_MONTH_GROUP]} ${match.groupValues[DATE_TIME_GROUP]}"
}

private val POST_DATE = Regex("(\\d{2})[/.](\\d{2})(?:[/.]\\d{2,4})?.*?(\\d{2}:\\d{2}(?::\\d{2})?)")

private const val DATE_MONTH_GROUP = 2
private const val DATE_TIME_GROUP = 3
