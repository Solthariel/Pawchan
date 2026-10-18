package com.solthariel.pawchan.data.drafts

import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import okhttp3.Call
import okhttp3.coroutines.executeAsync
import java.io.File
import java.io.IOException

internal suspend fun downloadArchiveResponse(
    call: Call,
    part: File,
): Long =
    coroutineScope {
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
                if (response.code != HTTP_OK) throw IOException("Медиа: HTTP ${response.code}")
                val expected = response.body.contentLength()
                if (expected > MAX_ARCHIVE_FILE_BYTES || requireNotNull(part.parentFile).usableSpace <
                    MIN_FREE_ARCHIVE_BYTES + expected.coerceAtLeast(0)
                ) {
                    throw IOException("Недостаточно места или файл слишком большой")
                }
                val bytes =
                    response.body.byteStream().use { input ->
                        part.outputStream().use { output ->
                            archiveCopyBytes(input, output, MAX_ARCHIVE_FILE_BYTES)
                        }
                    }
                if (bytes == 0L ||
                    (expected >= 0 && expected != bytes)
                ) {
                    throw IOException("Медиа загружено не полностью")
                }
                bytes
            }
        } finally {
            cancellation.cancel()
        }
    }

private const val HTTP_OK = 200
private const val MAX_ARCHIVE_FILE_BYTES = 512L * 1024 * 1024
private const val MIN_FREE_ARCHIVE_BYTES = 32L * 1024 * 1024
