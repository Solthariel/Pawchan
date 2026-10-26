package com.solthariel.pawchan.composer

import com.solthariel.pawchan.domain.posting.PostingFailure

internal fun PostingFailure.toReplyError(): Pair<ReplyError, String?> =
    when (this) {
        PostingFailure.NetworkUnavailable -> {
            ReplyError.NETWORK to null
        }

        PostingFailure.NetworkTimeout -> {
            ReplyError.TIMEOUT to null
        }

        PostingFailure.SecureConnectionFailed -> {
            ReplyError.SECURE_CONNECTION to null
        }

        PostingFailure.SubmissionUnconfirmed -> {
            ReplyError.SUBMISSION_UNCONFIRMED to null
        }

        PostingFailure.InvalidResponse -> {
            ReplyError.INVALID_RESPONSE to null
        }

        PostingFailure.CaptchaRejected -> {
            ReplyError.CAPTCHA to null
        }

        PostingFailure.ThreadClosed -> {
            ReplyError.THREAD_CLOSED to null
        }

        is PostingFailure.HttpStatus -> {
            ReplyError.SERVER to
                if (status == COMPOSER_RATE_LIMIT_STATUS) "Сервер ограничил запросы. Повторите позже" else null
        }

        is PostingFailure.Rejected -> {
            ReplyError.REJECTED to reason
        }
    }

internal const val COMPOSER_RATE_LIMIT_STATUS = 429
