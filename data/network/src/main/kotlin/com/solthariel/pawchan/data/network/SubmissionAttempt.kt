package com.solthariel.pawchan.data.network

import com.solthariel.pawchan.domain.posting.PostingFailure
import com.solthariel.pawchan.domain.posting.PostingOutcome
import okhttp3.Call
import okhttp3.Request
import okhttp3.RequestBody
import okio.BufferedSink
import java.util.concurrent.atomic.AtomicBoolean

/** One explicit reply/thread attempt; captcha and voting do not use this policy. */
internal suspend fun <T> executeSubmissionRequest(
    callFactory: Call.Factory,
    request: Request,
    decode: (String) -> PostingOutcome<T>,
): PostingOutcome<T> {
    val attempt = SubmissionAttempt()
    val body = requireNotNull(request.body)
    val guarded = request.newBuilder().method(request.method, attempt.body(body)).build()
    val outcome = executePostingRequest(callFactory, guarded, decode)
    if (outcome !is PostingOutcome.Failure) return outcome
    return if (attempt.unconfirmed(outcome.reason)) {
        PostingOutcome.Failure(PostingFailure.SubmissionUnconfirmed)
    } else {
        outcome
    }
}

/** Writing may reach the server before it fails; no claim of successful delivery is made. */
private class SubmissionAttempt {
    private val bodyStarted = AtomicBoolean()

    fun body(delegate: RequestBody): RequestBody =
        object : RequestBody() {
            override fun contentType() = delegate.contentType()

            override fun contentLength() = delegate.contentLength()

            override fun isDuplex() = delegate.isDuplex()

            // The server has no idempotency key: never transparently replay a POST body.
            override fun isOneShot() = true

            override fun writeTo(sink: BufferedSink) {
                bodyStarted.set(true)
                delegate.writeTo(sink)
            }
        }

    fun unconfirmed(reason: PostingFailure): Boolean =
        when (reason) {
            PostingFailure.NetworkTimeout,
            PostingFailure.NetworkUnavailable,
            PostingFailure.SecureConnectionFailed,
            -> bodyStarted.get()

            PostingFailure.InvalidResponse -> true

            is PostingFailure.HttpStatus -> reason.status in SERVER_ERROR_RANGE

            is PostingFailure.Rejected -> reason.reason.isNullOrBlank()

            else -> false
        }
}

private val SERVER_ERROR_RANGE = FIRST_SERVER_ERROR_STATUS..LAST_SERVER_ERROR_STATUS
private const val FIRST_SERVER_ERROR_STATUS = 500
private const val LAST_SERVER_ERROR_STATUS = 599
