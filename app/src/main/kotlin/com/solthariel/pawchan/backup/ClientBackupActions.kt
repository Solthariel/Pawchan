package com.solthariel.pawchan.backup

import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.solthariel.pawchan.data.maintenance.BackupPasswordException
import com.solthariel.pawchan.data.maintenance.BackupProtection
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.launch
import java.io.IOException

internal class ClientBackupActions(
    private val service: ClientBackupService,
    private val scope: CoroutineScope,
    private val onRestored: () -> Unit,
) {
    var busy by mutableStateOf(false)
        private set
    var message by mutableStateOf<String?>(null)
        private set
    var preview by mutableStateOf<BackupPreview?>(null)
        private set
    var passwordNeeded by mutableStateOf(false)
        private set
    var exportReady by mutableStateOf(false)
        private set
    private var exportBytes: ByteArray? = null
    private var protectedBytes: ByteArray? = null

    fun prepare(
        sections: Set<BackupSection>,
        password: CharArray?,
    ) {
        if (busy) {
            password?.fill('\u0000')
            return
        }
        execute {
            try {
                exportBytes?.fill(0)
                exportBytes = service.prepare(sections, password)
                exportReady = true
            } finally {
                password?.fill('\u0000')
            }
        }
    }

    fun save(uri: Uri?) =
        execute {
            val bytes = exportBytes ?: throw IOException("Подготовьте копию заново")
            exportBytes = null
            exportReady = false
            try {
                if (uri != null) {
                    service.write(uri, bytes)
                    message = "Резервная копия сохранена"
                }
            } finally {
                bytes.fill(0)
            }
        }

    fun read(uri: Uri) =
        execute {
            discard()
            val bytes = service.read(uri)
            try {
                if (BackupProtection.isEncrypted(bytes)) {
                    protectedBytes = bytes
                    passwordNeeded = true
                } else {
                    preview = service.inspect(bytes, null)
                }
            } finally {
                if (protectedBytes !== bytes) bytes.fill(0)
            }
        }

    fun unlock(password: CharArray) {
        if (busy) {
            password.fill('\u0000')
            return
        }
        execute {
            try {
                preview = service.inspect(requireNotNull(protectedBytes), password)
                protectedBytes?.fill(0)
                protectedBytes = null
                passwordNeeded = false
            } finally {
                password.fill('\u0000')
            }
        }
    }

    fun restore(sections: Set<BackupSection>) =
        execute {
            service.restore(requireNotNull(preview).backup, sections)
            preview = null
            onRestored()
            message = "Выбранные разделы заменены данными из копии"
        }

    fun discard() {
        preview = null
        passwordNeeded = false
        protectedBytes?.fill(0)
        protectedBytes = null
        message = null
    }

    fun close() {
        discard()
        exportBytes?.fill(0)
        exportBytes = null
        exportReady = false
    }

    private fun execute(operation: suspend () -> Unit) {
        if (busy) return
        busy = true
        message = null
        scope.launch(start = CoroutineStart.UNDISPATCHED) {
            try {
                operation()
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (failure: BackupPasswordException) {
                message = failure.message
            } catch (_: SecurityException) {
                message = "Нет доступа к файлу"
            } catch (failure: IOException) {
                message = "Ошибка чтения или записи: ${failure.message}. " +
                    "Если восстановление прервано, перезапустите приложение для проверки и отката."
            } catch (failure: IllegalArgumentException) {
                message = failure.message ?: "Повреждённая или несовместимая копия"
            } finally {
                busy = false
            }
        }
    }
}
