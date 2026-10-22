package com.solthariel.pawchan.copyfiles

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.launch
import java.io.IOException
import java.io.OutputStream

internal class CopyFileActions(
    val store: CopyFileStore,
    val tree: String?,
    val kind: CopyFileKind,
    private val scope: CoroutineScope,
    val ready: Boolean = true,
) {
    var busy by mutableStateOf(false)
        private set
    var browsing by mutableStateOf(false)
        private set
    var documents by mutableStateOf<List<CopyDocument>>(emptyList())
        private set
    var saved by mutableStateOf<CopyDocument?>(null)
        private set
    var error by mutableStateOf<String?>(null)
        private set

    fun browse() {
        browsing = true
        documents = emptyList()
        execute { documents = store.list(requireNotNull(tree), kind) }
    }

    fun save(
        name: String,
        writer: suspend (OutputStream) -> Unit,
    ) = execute {
        saved = store.write(requireNotNull(tree), kind, name, writer)
    }

    fun closeList() {
        browsing = false
    }

    fun closeResult() {
        saved = null
    }

    fun clearError() {
        error = null
    }

    private fun execute(operation: suspend () -> Unit) {
        if (busy) return
        busy = true
        error = null
        scope.launch(start = CoroutineStart.UNDISPATCHED) {
            try {
                operation()
            } catch (_: SecurityException) {
                error = "Доступ к папке потерян. Выберите папку заново в настройках или другой файл."
            } catch (failure: IOException) {
                error = failure.message ?: "Ошибка чтения или записи копии"
            } catch (_: IllegalArgumentException) {
                error = "Папка недоступна. Выберите её заново в настройках."
            } finally {
                busy = false
            }
        }
    }
}
