package com.solthariel.pawchan.thread

import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.solthariel.pawchan.core.ui.LocalPostDisplayPreferences
import com.solthariel.pawchan.core.ui.PostHeaderAuthor
import com.solthariel.pawchan.core.ui.PostHeaderAuthorUi
import com.solthariel.pawchan.core.ui.PostHeaderIndex
import com.solthariel.pawchan.core.ui.PostHeaderLayout
import com.solthariel.pawchan.core.ui.PostHeaderText
import com.solthariel.pawchan.core.ui.PostStateMarkers
import com.solthariel.pawchan.core.ui.formatPostTime
import com.solthariel.pawchan.media.thumbnail.DeferredThumbnail
import com.solthariel.pawchan.media.thumbnail.ThumbnailLoading

/** Post header shared with archive; site badges and rating remain source-specific slots. */
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
    PostHeaderLayout(
        leading = {
            PostHeaderIndex(post.metadata.changes.deleted, display.index)
            PostHeaderText("#${post.id}", secondary)
            date?.let { PostHeaderText(it, secondary) }
            if (preferences.showStateMarkers) PostStateMarkers(display.markers, byline.email)
        },
        byline = {
            display.badges.forEach { badge ->
                val symbol = badge.symbol
                if (symbol != null) {
                    Text(symbol, fontSize = 12.sp, maxLines = 1)
                } else {
                    DeferredThumbnail(badge.image.value, loading, badge.label, Modifier.size(14.dp))
                }
            }
            PostHeaderAuthor(
                PostHeaderAuthorUi(
                    name = byline.author ?: byline.defaultName?.takeIf { preferences.showDefaultName },
                    color = byline.authorColor,
                    tripcode = byline.tripcode,
                ),
            )
        },
        trailing = { post.metadata.rating?.let { PostRatingButtons(post.id, it) } },
    )
}
