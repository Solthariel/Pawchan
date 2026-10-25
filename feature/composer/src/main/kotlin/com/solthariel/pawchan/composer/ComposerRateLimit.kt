package com.solthariel.pawchan.composer

import android.os.SystemClock
import com.solthariel.pawchan.domain.posting.PostingFailure

/** Owned by one composer session, independent of drafts and UI recomposition. */
internal class ComposerRateLimit(
    private val nowMillis: () -> Long = SystemClock::elapsedRealtime,
) {
    private var deadline: Long? = null

    fun activeFailure(): ComposerFailureState? {
        val until = deadline ?: return null
        return if (cooldownRemainingSeconds(until, nowMillis()) > 0L) {
            ComposerFailureState(ReplyError.SERVER, retryDeadlineElapsedMillis = until)
        } else {
            null
        }
    }

    fun failure(reason: PostingFailure): ComposerFailureState {
        val delay =
            (reason as? PostingFailure.HttpStatus)
                ?.takeIf { it.status == COMPOSER_RATE_LIMIT_STATUS }
                ?.retryAfterMillis
        if (delay != null && delay > 0L) {
            val now = nowMillis()
            val until = now + delay.coerceAtMost(Long.MAX_VALUE - now)
            deadline = maxOf(deadline ?: until, until)
            return ComposerFailureState(ReplyError.SERVER, retryDeadlineElapsedMillis = deadline)
        }
        val (error, message) = reason.toReplyError()
        return ComposerFailureState(error, message)
    }
}

internal fun cooldownRemainingSeconds(
    deadline: Long,
    nowMillis: Long,
): Long {
    val millis = (deadline - nowMillis).coerceAtLeast(0L)
    return millis / COOLDOWN_SECOND + if (millis % COOLDOWN_SECOND == 0L) 0L else 1L
}

private const val COOLDOWN_SECOND = 1_000L
