package com.solthariel.pawchan.data.network

import com.solthariel.pawchan.domain.model.DomainFailure
import com.solthariel.pawchan.domain.model.DomainOutcome
import com.solthariel.pawchan.domain.posting.PostingFailure
import com.solthariel.pawchan.domain.posting.PostingOutcome
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerializationException
import okhttp3.Call
import okhttp3.Request
import okhttp3.Response
import okhttp3.coroutines.executeAsync
import java.io.IOException
import java.net.SocketTimeoutException
import javax.net.ssl.SSLException

internal data class HttpTransportFailure(
    val kind: Kind,
    val retryAfterMillis: Long? = null,
) {
    enum class Kind { TIMEOUT, SECURE_CONNECTION, NETWORK, RATE_LIMITED }

    companion object {
        val TIMEOUT = HttpTransportFailure(Kind.TIMEOUT)
        val SECURE_CONNECTION = HttpTransportFailure(Kind.SECURE_CONNECTION)
        val NETWORK = HttpTransportFailure(Kind.NETWORK)
        val RATE_LIMITED = HttpTransportFailure(Kind.RATE_LIMITED)
    }
}

internal suspend fun <T> Call.Factory.executeHttpCall(
    request: Request,
    onTransportFailure: (HttpTransportFailure) -> T,
    handleResponse: (Response) -> T,
): T =
    withContext(Dispatchers.IO) {
        try {
            awaitServerAvailability(request.url.host)
            newCall(request).executeAsync().use(handleResponse)
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (cooldown: ServerCoolingDownException) {
            onTransportFailure(HttpTransportFailure.RATE_LIMITED.copy(retryAfterMillis = cooldown.retryAfterMillis))
        } catch (_: SocketTimeoutException) {
            onTransportFailure(HttpTransportFailure.TIMEOUT)
        } catch (_: SSLException) {
            onTransportFailure(HttpTransportFailure.SECURE_CONNECTION)
        } catch (_: IOException) {
            onTransportFailure(HttpTransportFailure.NETWORK)
        }
    }

internal suspend fun <T> executeDomainRequest(
    callFactory: Call.Factory,
    request: Request,
    decode: (String) -> T?,
): DomainOutcome<T> =
    callFactory.executeHttpCall(
        request = request,
        onTransportFailure = HttpTransportFailure::toDomainOutcome,
        handleResponse = { response ->
            response.toDomainOutcome(callFactory.serverBackoff()?.remainingMillis(request.url.host), decode)
        },
    )

internal suspend fun <T> executePostingRequest(
    callFactory: Call.Factory,
    request: Request,
    decode: (String) -> PostingOutcome<T>,
): PostingOutcome<T> =
    callFactory.executeHttpCall(
        request = request,
        onTransportFailure = HttpTransportFailure::toPostingOutcome,
        handleResponse = { response ->
            response.toPostingOutcome(callFactory.serverBackoff()?.remainingMillis(request.url.host), decode)
        },
    )

internal fun <T> Response.toDomainOutcome(
    cooldownMillis: Long? = null,
    decode: (String) -> T?,
): DomainOutcome<T> {
    val payload = body.string()
    val challenge = ProtectionChallengeDetector.detect(this, payload)
    return when {
        code == NOT_FOUND_STATUS || code == GONE_STATUS -> DomainOutcome.Failure(DomainFailure.NotFound)
        challenge != null -> DomainOutcome.Failure(challenge)
        !isSuccessful -> DomainOutcome.Failure(DomainFailure.HttpStatus(code, responseCooldownMillis(cooldownMillis)))
        else -> decodeDomainPayload(payload, decode)
    }
}

private fun <T> decodeDomainPayload(
    payload: String,
    decode: (String) -> T?,
): DomainOutcome<T> =
    try {
        decode(payload)?.let(DomainOutcome<T>::Success)
            ?: DomainOutcome.Failure(DomainFailure.InvalidResponse)
    } catch (_: SerializationException) {
        DomainOutcome.Failure(DomainFailure.InvalidResponse)
    }

internal fun <T> Response.toPostingOutcome(
    cooldownMillis: Long? = null,
    decode: (String) -> PostingOutcome<T>,
): PostingOutcome<T> {
    if (!isSuccessful) {
        return PostingOutcome.Failure(PostingFailure.HttpStatus(code, responseCooldownMillis(cooldownMillis)))
    }
    return try {
        decode(body.string())
    } catch (_: SerializationException) {
        PostingOutcome.Failure(PostingFailure.InvalidResponse)
    }
}

internal fun <T> HttpTransportFailure.toDomainOutcome(): DomainOutcome<T> =
    DomainOutcome.Failure(
        when (kind) {
            HttpTransportFailure.Kind.TIMEOUT -> DomainFailure.NetworkTimeout
            HttpTransportFailure.Kind.SECURE_CONNECTION -> DomainFailure.SecureConnectionFailed
            HttpTransportFailure.Kind.NETWORK -> DomainFailure.NetworkUnavailable
            HttpTransportFailure.Kind.RATE_LIMITED -> DomainFailure.HttpStatus(RATE_LIMITED_STATUS, retryAfterMillis)
        },
    )

private fun <T> HttpTransportFailure.toPostingOutcome(): PostingOutcome<T> =
    PostingOutcome.Failure(
        when (kind) {
            HttpTransportFailure.Kind.TIMEOUT -> PostingFailure.NetworkTimeout
            HttpTransportFailure.Kind.SECURE_CONNECTION -> PostingFailure.SecureConnectionFailed
            HttpTransportFailure.Kind.NETWORK -> PostingFailure.NetworkUnavailable
            HttpTransportFailure.Kind.RATE_LIMITED -> PostingFailure.HttpStatus(RATE_LIMITED_STATUS, retryAfterMillis)
        },
    )

private const val NOT_FOUND_STATUS = 404
private const val GONE_STATUS = 410

private fun Response.responseCooldownMillis(knownRemaining: Long?): Long? =
    if (code == RATE_LIMITED_STATUS) {
        knownRemaining?.takeIf { it > 0L }
            ?: retryAfterMillis(header("Retry-After"), header("Date"), System.currentTimeMillis())
    } else {
        null
    }
