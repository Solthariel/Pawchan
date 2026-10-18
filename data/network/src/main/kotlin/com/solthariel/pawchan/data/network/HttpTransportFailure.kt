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

internal enum class HttpTransportFailure {
    TIMEOUT,
    SECURE_CONNECTION,
    NETWORK,
    RATE_LIMITED,
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
        } catch (_: ServerCoolingDownException) {
            onTransportFailure(HttpTransportFailure.RATE_LIMITED)
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
        handleResponse = { response -> response.toDomainOutcome(decode) },
    )

internal suspend fun <T> executePostingRequest(
    callFactory: Call.Factory,
    request: Request,
    decode: (String) -> PostingOutcome<T>,
): PostingOutcome<T> =
    callFactory.executeHttpCall(
        request = request,
        onTransportFailure = HttpTransportFailure::toPostingOutcome,
        handleResponse = { response -> response.toPostingOutcome(decode) },
    )

internal fun <T> Response.toDomainOutcome(decode: (String) -> T?): DomainOutcome<T> {
    val payload = body.string()
    val challenge = ProtectionChallengeDetector.detect(this, payload)
    return when {
        code == NOT_FOUND_STATUS -> DomainOutcome.Failure(DomainFailure.NotFound)
        challenge != null -> DomainOutcome.Failure(challenge)
        !isSuccessful -> DomainOutcome.Failure(DomainFailure.HttpStatus(code))
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

internal fun <T> Response.toPostingOutcome(decode: (String) -> PostingOutcome<T>): PostingOutcome<T> {
    if (!isSuccessful) {
        return PostingOutcome.Failure(PostingFailure.HttpStatus(code))
    }
    return try {
        decode(body.string())
    } catch (_: SerializationException) {
        PostingOutcome.Failure(PostingFailure.InvalidResponse)
    }
}

internal fun <T> HttpTransportFailure.toDomainOutcome(): DomainOutcome<T> =
    DomainOutcome.Failure(
        when (this) {
            HttpTransportFailure.TIMEOUT -> DomainFailure.NetworkTimeout
            HttpTransportFailure.SECURE_CONNECTION -> DomainFailure.SecureConnectionFailed
            HttpTransportFailure.NETWORK -> DomainFailure.NetworkUnavailable
            HttpTransportFailure.RATE_LIMITED -> DomainFailure.HttpStatus(RATE_LIMITED_STATUS)
        },
    )

private fun <T> HttpTransportFailure.toPostingOutcome(): PostingOutcome<T> =
    PostingOutcome.Failure(
        when (this) {
            HttpTransportFailure.TIMEOUT -> PostingFailure.NetworkTimeout
            HttpTransportFailure.SECURE_CONNECTION -> PostingFailure.SecureConnectionFailed
            HttpTransportFailure.NETWORK -> PostingFailure.NetworkUnavailable
            HttpTransportFailure.RATE_LIMITED -> PostingFailure.HttpStatus(RATE_LIMITED_STATUS)
        },
    )

private const val NOT_FOUND_STATUS = 404
