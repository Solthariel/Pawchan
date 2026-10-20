package com.solthariel.pawchan.infrastructure.media

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.edit
import androidx.core.net.toUri
import androidx.work.WorkInfo
import androidx.work.WorkManager
import com.solthariel.pawchan.domain.settings.AppSettingsRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import java.util.concurrent.ExecutionException
import java.util.concurrent.atomic.AtomicBoolean

/** App-lifetime owner. Only previously selected download trees are retired, never unrelated grants. */
internal class DownloadDirectoryPermissions(
    context: Context,
    private val settings: AppSettingsRepository,
    private val scope: CoroutineScope,
) {
    private val app = context.applicationContext
    private val resolver = app.contentResolver
    private val preferences = app.getSharedPreferences("download-directory-grants", Context.MODE_PRIVATE)
    private val manager by lazy { WorkManager.getInstance(app) }
    private val lock = Any()
    private val users = mutableMapOf<String, Int>()
    private val changes = MutableStateFlow(0L)
    private val started = AtomicBoolean(false)

    fun start() {
        if (!started.compareAndSet(false, true)) return
        scope.launch {
            combine(
                settings.appSettings.map { it.behavior.client.downloads.treeUri }.distinctUntilChanged(),
                manager.getWorkInfosByTagFlow("neko-media"),
                changes,
            ) { directory, _, _ -> directory }.collect { selected ->
                synchronized(lock) { retireUnused(selected) }
            }
        }
    }

    fun useDirectory(directory: String): AutoCloseable {
        synchronized(lock) {
            val retired = preferences.getStringSet(RETIRED, emptySet()).orEmpty()
            if (directory in retired && (users[directory] ?: 0) == 0) {
                throw SecurityException("Папка загрузок уже изменена")
            }
            users[directory] = (users[directory] ?: 0) + 1
        }
        val closed = AtomicBoolean(false)
        return AutoCloseable {
            if (closed.compareAndSet(false, true)) {
                synchronized(lock) {
                    val count = (users[directory] ?: 1) - 1
                    if (count == 0) users.remove(directory) else users[directory] = count
                    changes.value++
                }
            }
        }
    }

    fun closeAfterCommit(lease: AutoCloseable) {
        // Never block a WorkManager DB completion thread on our cleanup lock.
        scope.launch { lease.close() }
    }

    private fun retireUnused(selected: String?) {
        val retired = preferences.getStringSet(RETIRED, emptySet()).orEmpty().toMutableSet()
        val previous = preferences.getString(SELECTED, null)
        if (previous != selected && previous != null) retired.add(previous)
        retired.remove(selected)
        // Persist the retirement before touching a grant so process death cannot lose pending work.
        if (!store(selected, retired)) return
        val active = activeDestinations() ?: return
        val leased = users.filterValues { it > 0 }.keys
        retired.removeAll { uri ->
            canReleaseDownloadGrant(uri, selected, active, leased) && release(uri.toUri())
        }
        store(selected, retired)
    }

    private fun activeDestinations(): List<String?>? =
        try {
            // Query the DB, not the last flow snapshot: an enqueue operation may just have committed.
            manager
                .getWorkInfosByTag("neko-media")
                .get()
                .filterNot { it.state.isFinished }
                .map(::destination)
        } catch (_: ExecutionException) {
            null
        } catch (_: InterruptedException) {
            Thread.currentThread().interrupt()
            null
        }

    private fun release(uri: Uri): Boolean =
        try {
            val grant = resolver.persistedUriPermissions.firstOrNull { it.uri == uri }
            if (grant != null) {
                val flags =
                    (if (grant.isReadPermission) Intent.FLAG_GRANT_READ_URI_PERMISSION else 0) or
                        (if (grant.isWritePermission) Intent.FLAG_GRANT_WRITE_URI_PERMISSION else 0)
                if (flags != 0) resolver.releasePersistableUriPermission(uri, flags)
            }
            true
        } catch (_: SecurityException) {
            false
        } catch (_: IllegalArgumentException) {
            false
        }

    private fun store(
        selected: String?,
        retired: Set<String>,
    ): Boolean {
        var persisted = false
        preferences.edit {
            putString(SELECTED, selected)
            putStringSet(RETIRED, retired)
            // Like AppLockStore, check durability; KTX's subsequent apply has no pending changes.
            persisted = commit()
        }
        return persisted
    }
}

private fun destination(work: WorkInfo): String? =
    work.tags.firstOrNull { it.startsWith("media-destination:") }?.removePrefix("media-destination:")

private const val SELECTED = "selected"
private const val RETIRED = "retired"
