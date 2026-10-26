package com.solthariel.pawchan.composer

import com.solthariel.pawchan.domain.posting.PostingFailure

/** Owned by a single form controller, not persisted in draft data. */
internal class ComposerSubmissionGuard {
    private var pending = false

    fun record(reason: PostingFailure) {
        if (reason == PostingFailure.SubmissionUnconfirmed) pending = true
    }

    fun activeFailure(): ComposerFailureState? {
        if (!pending) return null
        return ComposerFailureState(ReplyError.SUBMISSION_UNCONFIRMED)
    }

    fun acknowledge() {
        pending = false
    }
}

internal const val SUBMISSION_UNCONFIRMED_TEXT =
    "Отправка не подтверждена. Проверьте, появилось ли сообщение, перед повторной попыткой."
