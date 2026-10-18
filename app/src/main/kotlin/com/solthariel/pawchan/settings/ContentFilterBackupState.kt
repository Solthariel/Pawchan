package com.solthariel.pawchan.settings

import android.content.ContentResolver
import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.solthariel.pawchan.domain.filter.ContentFilterRule
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.IOException
import java.io.InputStream
import java.nio.ByteBuffer
import java.nio.charset.CodingErrorAction

internal class ContentFilterBackupState(
    private val resolver: ContentResolver,
    private val scope: CoroutineScope,
) {
    var rules: List<ContentFilterRule> = emptyList()
    var restore: (suspend (List<ContentFilterRule>) -> Int?)? = null
    var busy by mutableStateOf(false)
        private set
    var message by mutableStateOf<String?>(null)

    fun save(uri: Uri) {
        val snapshot = rules.toList()
        execute {
            withContext(Dispatchers.IO) {
                val text = encodeContentFilterBackup(snapshot)
                val stream = resolver.openOutputStream(uri, "wt") ?: throw IOException("Файл недоступен")
                stream.bufferedWriter(Charsets.UTF_8).use { it.write(text) }
            }
            "Сохранено правил: ${snapshot.size}"
        }
    }

    fun load(uri: Uri) {
        execute {
            val incoming =
                withContext(Dispatchers.IO) {
                    val stream = resolver.openInputStream(uri) ?: throw IOException("Файл недоступен")
                    stream.use { decodeContentFilterBackup(readBackupText(it)) }
                }
            val added = restore?.invoke(incoming) ?: throw IOException("Не удалось сохранить правила")
            "Добавлено правил: $added. Текущие правила сохранены, повторы пропущены."
        }
    }

    private fun execute(operation: suspend () -> String) {
        if (!busy) {
            busy = true
            scope.launch {
                try {
                    message = operation()
                } catch (error: CancellationException) {
                    throw error
                } catch (_: IOException) {
                    message = "Не удалось прочитать или записать файл. Проверьте доступ и свободное место."
                } catch (_: SecurityException) {
                    message = "Нет доступа к выбранному файлу"
                } catch (error: IllegalArgumentException) {
                    message = error.message ?: "Некорректный файл правил"
                } finally {
                    busy = false
                }
            }
        }
    }
}

internal fun readBackupText(stream: InputStream): String {
    val bytes = ByteArray(FILTER_BACKUP_MAX_BYTES + 1)
    var total = 0
    while (total < bytes.size) {
        val count = stream.read(bytes, total, bytes.size - total)
        if (count < 0) break
        require(count > 0) { "Не удалось прочитать файл" }
        total += count
    }
    require(total <= FILTER_BACKUP_MAX_BYTES) { "Файл больше 1 МБ" }
    return Charsets.UTF_8
        .newDecoder()
        .onMalformedInput(CodingErrorAction.REPORT)
        .onUnmappableCharacter(CodingErrorAction.REPORT)
        .decode(ByteBuffer.wrap(bytes, 0, total))
        .toString()
}
