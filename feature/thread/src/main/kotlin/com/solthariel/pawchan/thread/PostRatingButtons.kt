package com.solthariel.pawchan.thread

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.solthariel.pawchan.core.ui.UiRatingIcon
import com.solthariel.pawchan.core.ui.ratingDislikeColor
import com.solthariel.pawchan.core.ui.ratingLikeColor
import com.solthariel.pawchan.domain.model.PostAddress
import com.solthariel.pawchan.domain.model.PostId
import com.solthariel.pawchan.domain.model.PostRating
import com.solthariel.pawchan.domain.thread.PostVote
import kotlinx.coroutines.launch

/**
 * Лайк/дизлайк в строке автора. Для каждого действия выделяется минимум 48dp;
 * соседние области не полагаются на расширение маленьких текстовых целей.
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
    TextButton(
        onClick = onClick,
        enabled = state.enabled,
        modifier =
            Modifier
                .heightIn(min = 48.dp)
                .widthIn(min = 48.dp)
                .semantics { contentDescription = "$label: ${state.count}" },
        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 8.dp),
        colors = ButtonDefaults.textButtonColors(contentColor = color),
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
            UiRatingIcon(state.up, state.voted, color, Modifier.size(18.dp))
            Text(
                state.count.toString(),
                style = MaterialTheme.typography.labelLarge,
            )
        }
    }
}
