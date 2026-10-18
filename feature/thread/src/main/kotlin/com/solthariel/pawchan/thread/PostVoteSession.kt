package com.solthariel.pawchan.thread

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.staticCompositionLocalOf
import com.solthariel.pawchan.domain.model.PostAddress
import com.solthariel.pawchan.domain.model.PostRating
import com.solthariel.pawchan.domain.posting.PostingFailure
import com.solthariel.pawchan.domain.posting.PostingOutcome
import com.solthariel.pawchan.domain.thread.PostVote
import com.solthariel.pawchan.domain.thread.PostVoteRepository

/**
 * Оценки постов на время жизни приложения: какие посты уже оценены (кнопки после этого неактивны)
 * и какие запросы ещё идут. Владелец — корень зависимостей приложения, не глобальный объект.
 */
class PostVoteSession(
    private val repository: PostVoteRepository,
) {
    private val votes = mutableStateMapOf<PostAddress, PostVoteRecord>()
    private val pending = mutableStateMapOf<PostAddress, PostVote>()

    internal fun state(
        post: PostAddress,
        rating: PostRating,
    ): PostVoteUiState {
        val record = votes[post]
        return PostVoteUiState(
            likes = record?.shown(rating.likes, PostVote.LIKE) ?: rating.likes,
            dislikes = record?.shown(rating.dislikes, PostVote.DISLIKE) ?: rating.dislikes,
            voted = record?.vote,
            pending = pending[post],
        )
    }

    /** null — успех или повторное нажатие; иначе текст ошибки сервера/сети. */
    internal suspend fun vote(
        post: PostAddress,
        vote: PostVote,
        rating: PostRating,
    ): String? {
        if (post in votes || post in pending) return null
        pending[post] = vote
        return try {
            when (val result = repository.vote(post, vote)) {
                is PostingOutcome.Success -> {
                    votes[post] = PostVoteRecord(vote, if (vote == PostVote.LIKE) rating.likes else rating.dislikes)
                    null
                }

                is PostingOutcome.Failure -> {
                    result.reason.voteMessage()
                }
            }
        } finally {
            pending.remove(post)
        }
    }
}

/** Сервер не возвращает новый счётчик: показываем base + 1, пока обновлённый тред не догонит. */
private data class PostVoteRecord(
    val vote: PostVote,
    val base: Int,
) {
    fun shown(
        server: Int,
        kind: PostVote,
    ): Int = if (vote == kind) maxOf(server, base + 1) else server
}

internal data class PostVoteUiState(
    val likes: Int,
    val dislikes: Int,
    val voted: PostVote?,
    val pending: PostVote?,
) {
    val enabled: Boolean get() = voted == null && pending == null
}

private fun PostingFailure.voteMessage(): String =
    when (this) {
        is PostingFailure.Rejected -> reason ?: "2ch отклонил оценку"
        PostingFailure.NetworkUnavailable -> "Нет соединения с 2ch"
        PostingFailure.NetworkTimeout -> "2ch не ответил вовремя"
        is PostingFailure.HttpStatus -> "Ошибка оценки: HTTP $status"
        else -> "Не удалось оценить пост"
    }

internal val LocalPostVotes = staticCompositionLocalOf<PostVoteSession?> { null }

@Composable
fun ThreadPostVoteHost(
    session: PostVoteSession?,
    content: @Composable () -> Unit,
) {
    CompositionLocalProvider(LocalPostVotes provides session, content = content)
}
