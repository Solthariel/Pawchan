package com.solthariel.pawchan.backup

import android.content.Context
import com.solthariel.pawchan.data.database.RoomStorage
import com.solthariel.pawchan.data.drafts.FileWorkspaceRepository
import com.solthariel.pawchan.data.maintenance.MaintenanceGate
import com.solthariel.pawchan.data.preferences.DataStoreSettingsRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File

internal data class BackupRecoveryState(
    val ready: Boolean = false,
    val error: String? = null,
)

/** Application-owned bootstrap barrier. No UI repositories/work scheduling until recovery finishes. */
internal class BackupRecovery(
    private val context: Context,
    private val settings: DataStoreSettingsRepository,
    private val scope: CoroutineScope,
    private val onReady: () -> Unit,
) {
    private val mutableState = MutableStateFlow(BackupRecoveryState())
    val state = mutableState.asStateFlow()
    private val running =
        java.util.concurrent.atomic
            .AtomicBoolean(false)

    private suspend fun recoverPending() {
        val gate = MaintenanceGate(File(context.filesDir, "backup-maintenance"))
        gate.recover {
            if (gate.journal.exists()) {
                RoomStorage.create(context).use { storage ->
                    val workspace = FileWorkspaceRepository(File(context.filesDir, "workspace"))
                    val stores = BackupStores(settings.portable, storage.library, workspace)
                    BackupUndoJournal(gate, stores).recover()
                }
            }
        }
    }

    private fun failed() {
        mutableState.value =
            BackupRecoveryState(
                error =
                    "Не удалось откатить прерванное восстановление. Данные не будут открыты для записи. " +
                        "Освободите место и повторите попытку.",
            )
    }

    fun start() {
        if (mutableState.value.ready || !running.compareAndSet(false, true)) return
        scope.launch {
            try {
                mutableState.value = BackupRecoveryState()
                recoverPending()
                onReady()
                mutableState.value = BackupRecoveryState(ready = true)
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: java.io.IOException) {
                failed()
            } catch (_: NoSuchElementException) {
                failed()
            } catch (_: IllegalArgumentException) {
                failed()
            } catch (_: IllegalStateException) {
                failed()
            } catch (_: SecurityException) {
                failed()
            } finally {
                running.set(false)
            }
        }
    }
}
