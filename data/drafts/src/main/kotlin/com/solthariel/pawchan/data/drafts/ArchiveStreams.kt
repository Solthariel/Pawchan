package com.solthariel.pawchan.data.drafts

import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream

internal suspend fun archiveCopyBytes(
    input: InputStream,
    output: OutputStream,
    limit: Long,
): Long {
    val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
    var total = 0L
    while (true) {
        currentCoroutineContext().ensureActive()
        val count = input.read(buffer)
        if (count < 0) break
        total += count
        if (total > limit) throw IOException("Превышен лимит размера архива")
        output.write(buffer, 0, count)
    }
    return total
}
