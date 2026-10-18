package com.solthariel.pawchan.data.network

import com.solthariel.pawchan.domain.model.DomainFailure
import com.solthariel.pawchan.domain.model.DomainOutcome
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.Call
import okhttp3.Request
import okhttp3.coroutines.executeAsync
import java.io.ByteArrayOutputStream
import java.io.IOException

internal suspend fun Call.Factory.arhivachPayload(request: Request): DomainOutcome<String> =
    when (val result = arhivachExchange(request)) {
        is DomainOutcome.Success -> {
            if (result.value.code in SUCCESS_CODES) {
                DomainOutcome.Success(result.value.bytes.toString(Charsets.UTF_8))
            } else {
                DomainOutcome.Failure(DomainFailure.HttpStatus(result.value.code))
            }
        }

        is DomainOutcome.Failure -> {
            result
        }
    }

/** Ответ Архивача целиком: код, адрес перенаправления и тело (для капчи — байты картинки). */
internal class ArhivachExchange(
    val code: Int,
    val location: String?,
    val bytes: ByteArray,
)

internal suspend fun Call.Factory.arhivachExchange(request: Request): DomainOutcome<ArhivachExchange> =
    withContext(Dispatchers.IO) {
        try {
            coroutineScope {
                val call = newCall(request)
                val cancellation =
                    launch(start = CoroutineStart.UNDISPATCHED) {
                        try {
                            awaitCancellation()
                        } finally {
                            call.cancel()
                        }
                    }
                try {
                    call.executeAsync().use { response ->
                        when (val body = readArchivePayload(response.body.byteStream())) {
                            is DomainOutcome.Success -> {
                                DomainOutcome.Success(
                                    ArhivachExchange(response.code, response.header("Location"), body.value),
                                )
                            }

                            is DomainOutcome.Failure -> {
                                body
                            }
                        }
                    }
                } finally {
                    cancellation.cancel()
                }
            }
        } catch (_: IOException) {
            DomainOutcome.Failure(DomainFailure.NetworkUnavailable)
        }
    }

private suspend fun readArchivePayload(input: java.io.InputStream): DomainOutcome<ByteArray> {
    val bytes = ByteArrayOutputStream()
    val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
    input.use {
        while (true) {
            currentCoroutineContext().ensureActive()
            val count = input.read(buffer)
            if (count < 0) break
            if (bytes.size() + count >
                MAX_ARCHIVE_HTML_BYTES
            ) {
                return DomainOutcome.Failure(DomainFailure.InvalidResponse)
            }
            bytes.write(buffer, 0, count)
        }
    }
    return DomainOutcome.Success(bytes.toByteArray())
}

private const val HTTP_OK = 200
private const val HTTP_LAST_SUCCESS = 299
private val SUCCESS_CODES = HTTP_OK..HTTP_LAST_SUCCESS

private const val MAX_ARCHIVE_HTML_BYTES = 32 * 1024 * 1024
