package com.solthariel.pawchan.thread

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.solthariel.pawchan.core.ui.LocalPostDisplayPreferences
import com.solthariel.pawchan.core.ui.PostStateMarkers
import com.solthariel.pawchan.core.ui.formatPostTime
import com.solthariel.pawchan.media.thumbnail.DeferredThumbnail
import com.solthariel.pawchan.media.thumbnail.ThumbnailLoading

/**
 * Заголовок поста (единая схема, см. AGENTS.md): порядковый номер, #номер, дата — начало строки
 * одной ширины, поэтому номера и даты стоят столбиком; дальше значки состояния,
 * значки сайта, имя и трипкод. Обрезается только имя; оценки прижаты вправо.
 */
@Composable
internal fun ThreadPostHeader(
    post: ThreadPostUi,
    loading: ThumbnailLoading,
) {
    val display = post.metadata.display
    val byline = display.byline
    val preferences = LocalPostDisplayPreferences.current
    val secondary = MaterialTheme.colorScheme.onSurfaceVariant
    val date =
        remember(byline.timestamp, byline.date, preferences) {
            byline.timestamp?.let { formatPostTime(it, preferences) } ?: byline.date
        }
    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(HEADER_GAP),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        PostIndex(post.metadata.changes.deleted, display.index)
        HeaderText("#${post.id}", secondary)
        date?.let { HeaderText(it, secondary) }
        if (preferences.showStateMarkers) PostStateMarkers(display.markers, byline.email)
        Row(
            Modifier.weight(1f),
            horizontalArrangement = Arrangement.spacedBy(HEADER_GAP),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            display.badges.forEach { badge ->
                val symbol = badge.symbol
                if (symbol != null) {
                    Text(symbol, fontSize = HEADER_TEXT, maxLines = 1)
                } else {
                    DeferredThumbnail(badge.image.value, loading, badge.label, Modifier.size(14.dp))
                }
            }
            HeaderAuthor(byline, preferences.showDefaultName)
        }
        post.metadata.rating?.let { PostRatingButtons(post.id, it) }
    }
}

@Composable
private fun RowScope.HeaderAuthor(
    byline: ThreadPostByline,
    showDefaultName: Boolean,
) {
    val name = byline.author ?: byline.defaultName?.takeIf { showDefaultName }
    val trip = byline.tripcode
    if (name == null && trip == null) return
    val color = byline.authorColor?.let(::Color) ?: MaterialTheme.colorScheme.onSurface
    val text =
        buildAnnotatedString {
            name?.let { withStyle(SpanStyle(color = color, fontWeight = FontWeight.Medium)) { append(it) } }
            if (name != null && trip != null) append(' ')
            trip?.let { withStyle(SpanStyle(color = MaterialTheme.colorScheme.onSurfaceVariant)) { append(it) } }
        }
    Text(
        text,
        Modifier.weight(1f, fill = false),
        fontSize = HEADER_TEXT,
        lineHeight = HEADER_LINE,
        maxLines = 1,
        softWrap = false,
        overflow = TextOverflow.Ellipsis,
    )
}

/** Удалённый пост: вместо порядкового номера красный «X». */
@Composable
private fun PostIndex(
    deleted: Boolean,
    index: Int,
) {
    if (deleted) {
        HeaderText(
            "X",
            MaterialTheme.colorScheme.error,
            Modifier.semantics { contentDescription = "Удалён" },
            weight = FontWeight.Bold,
        )
    } else {
        HeaderText(index.toString(), MaterialTheme.colorScheme.onSurface, weight = FontWeight.Bold)
    }
}

@Composable
private fun HeaderText(
    text: String,
    color: Color,
    modifier: Modifier = Modifier,
    weight: FontWeight? = null,
) {
    Text(
        text,
        modifier,
        color = color,
        fontSize = HEADER_TEXT,
        lineHeight = HEADER_LINE,
        fontWeight = weight,
        maxLines = 1,
        softWrap = false,
        overflow = TextOverflow.Ellipsis,
    )
}

private val HEADER_GAP = 3.dp
private val HEADER_TEXT = 12.sp
private val HEADER_LINE = 14.sp
