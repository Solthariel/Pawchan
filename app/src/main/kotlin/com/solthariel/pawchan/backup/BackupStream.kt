package com.solthariel.pawchan.backup

import java.io.ByteArrayOutputStream
import java.io.InputStream

internal fun readClientBackup(stream: InputStream): ByteArray {
    val output = ByteArrayOutputStream()
    val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
    while (true) {
        val count = stream.read(buffer)
        if (count < 0) break
        require(count > 0 && output.size() + count <= CLIENT_BACKUP_MAX_BYTES) { "Файл больше 32 МБ или недоступен" }
        output.write(buffer, 0, count)
    }
    return output.toByteArray()
}
