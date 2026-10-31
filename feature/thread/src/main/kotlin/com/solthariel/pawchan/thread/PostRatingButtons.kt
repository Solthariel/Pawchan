package com.solthariel.pawchan.thread

import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.solthariel.pawchan.core.ui.UiRatingIcon
import com.solthariel.pawchan.core.ui.ratingDislikeColor
import com.solthariel.pawchan.core.ui.ratingLikeColor
import com.solthariel.pawchan.domain.model.PostAddress
import com.solthariel.pawchan.domain.model.PostId
import com.solthariel.pawchan.domain.model.PostRating
import com.solthariel.pawchan.domain.thread.PostVote
import kotlinx.coroutines.launch

/**
 * Лайк/дизлайк в конце строки заголовка. Высота как у текста строки; область нажатия
 * расширяется самим Compose до минимального размера касания, строка не растёт.
 */
@Composable
internal fun PostRatingButtons(
    postId: Long,
    rating: PostRating,
) {
    val session = LocalPostVotes.current
    val thread = LocalPostLinkAddress.current
    val post = PostId.parse(postId)?.let { id -> thread?.let { PostAddress(it, id) } }
    val state = if (session != null && post != null) session.state(post, rating) else null
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val vote = { kind: PostVote ->
        if (session != null && post != null) {
            scope.launch {
                session.vote(post, kind, rating)?.let { Toast.makeText(context, it, Toast.LENGTH_LONG).show() }
            }
        }
    }
    val enabled = state?.enabled == true
    Row(horizontalArrangement = Arrangement.spacedBy(2.dp), verticalAlignment = Alignment.CenterVertically) {
        RatingButton(
            RatingButtonState(true, state?.likes ?: rating.likes, state?.voted == PostVote.LIKE, enabled),
            ratingLikeColor(),
            "Нравится",
        ) { vote(PostVote.LIKE) }
        RatingButton(
            RatingButtonState(false, state?.dislikes ?: rating.dislikes, state?.voted == PostVote.DISLIKE, enabled),
            ratingDislikeColor(),
            "Не нравится",
        ) { vote(PostVote.DISLIKE) }
    }
}

private data class RatingButtonState(
    val up: Boolean,
    val count: Int,
    val voted: Boolean,
    val enabled: Boolean,
)

@Composable
private fun RatingButton(
    state: RatingButtonState,
    color: Color,
    label: String,
    onClick: () -> Unit,
) {
    val iconSize = with(LocalDensity.current) { RATING_TEXT_SIZE.sp.toDp() }
    Row(
        Modifier
            .clip(RoundedCornerShape(4.dp))
            .clickable(enabled = state.enabled, role = Role.Button, onClick = onClick)
            .semantics { contentDescription = "$label: ${state.count}" }
            .padding(horizontal = 3.dp),
        horizontalArrangement = Arrangement.spacedBy(2.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        UiRatingIcon(state.up, state.voted, color, Modifier.size(iconSize))
        Text(state.count.toString(), color = color, fontSize = RATING_TEXT_SIZE.sp, lineHeight = 14.sp, maxLines = 1)
    }
}

private const val RATING_TEXT_SIZE = 12
