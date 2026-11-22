package com.solthariel.pawchan.copyfiles

import android.content.Context
import android.content.Intent
import android.database.Cursor
import android.provider.OpenableColumns
import androidx.core.net.toUri
import com.solthariel.pawchan.data.preferences.ZipIndexStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import java.io.IOException

internal data class IndexedZip(
    val route: String,
    val document: CopyDocument,
    val available: Boolean,
)

/** App-owned registry. A remembered URI is not evidence that the file still exists. */
internal class ZipRegistry(
    context: Context,
    private val scope: CoroutineScope,
    private val preferencesName: String = "exported-thread-zips",
) {
    private val app = context.applicationContext
    private val store = ZipIndexStore.create(app, preferencesName, scope)
    private val mutex = Mutex()
    private val mutableFiles = MutableStateFlow<List<IndexedZip>>(emptyList())
    val files = mutableFiles.asStateFlow()

    fun refresh() {
        scope.launch { mutex.withLock { logStorageFailure { loadAndVerify() } } }
    }

    fun record(
        route: String,
        document: CopyDocument,
        persistGrant: Boolean,
    ) {
        scope.launch {
            mutex.withLock {
                logStorageFailure {
                    withContext(Dispatchers.IO) {
                        if (persistGrant) {
                            try {
                                app.contentResolver.takePersistableUriPermission(
                                    document.uri.toUri(),
                                    Intent.FLAG_GRANT_READ_URI_PERMISSION,
                                )
                            } catch (_: SecurityException) {
                                // Some providers grant only temporary access.
                            } catch (_: IllegalArgumentException) {
                                // The provider may not support persistable grants.
                            }
                        }
                        store.update { stored -> appendDocument(stored, route, document) }
                    }
                    loadAndVerify()
                }
            }
        }
    }

    private suspend fun loadAndVerify() {
        mutableFiles.value = withContext(Dispatchers.IO) { read().map(::verify) }
    }

    private suspend fun read(): List<IndexedZip> {
        val stored = store.read() ?: return emptyList()
        val array = JSONArray(stored)
        return (0 until array.length()).mapNotNull { index -> decode(array.optJSONObject(index)) }
    }

    private suspend fun logStorageFailure(action: suspend () -> Unit) {
        try {
            action()
        } catch (error: IOException) {
            android.util.Log.e("ZipRegistry", "Cannot access ZIP index", error)
        } catch (error: JSONException) {
            android.util.Log.e("ZipRegistry", "Cannot decode ZIP index", error)
        } catch (error: ClassCastException) {
            android.util.Log.e("ZipRegistry", "Invalid ZIP index storage", error)
        }
    }

    private fun verify(item: IndexedZip): IndexedZip {
        return try {
            val uri = item.document.uri.toUri()
            val descriptor = app.contentResolver.openFileDescriptor(uri, "r") ?: return item.copy(available = false)
            descriptor.use { }
            val document =
                app.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                    if (cursor.moveToFirst()) item.document.withMetadata(cursor) else item.document
                } ?: item.document
            item.copy(document = document, available = true)
        } catch (_: IOException) {
            item.copy(available = false)
        } catch (_: SecurityException) {
            item.copy(available = false)
        } catch (_: IllegalArgumentException) {
            item.copy(available = false)
        } catch (_: IllegalStateException) {
            item.copy(available = false)
        } catch (_: UnsupportedOperationException) {
            item.copy(available = false)
        }
    }

    private fun CopyDocument.withMetadata(cursor: Cursor): CopyDocument {
        val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
        val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)
        return copy(
            name = if (nameIndex >= 0 && !cursor.isNull(nameIndex)) cursor.getString(nameIndex) else name,
            bytes = if (sizeIndex >= 0 && !cursor.isNull(sizeIndex)) cursor.getLong(sizeIndex) else bytes,
        )
    }
}

private fun appendDocument(
    stored: String?,
    route: String,
    document: CopyDocument,
): String {
    val existing = if (stored == null) JSONArray() else JSONArray(stored)
    val updated = JSONArray()
    for (index in 0 until existing.length()) {
        val entry = existing.get(index)
        if (entry !is JSONObject || entry.optString("uri") != document.uri) updated.put(entry)
    }
    updated.put(
        JSONObject()
            .put("route", route)
            .put("uri", document.uri)
            .put("name", document.name)
            .put("modified", document.modified)
            .put("bytes", document.bytes),
    )
    return updated.toString()
}

private fun decode(json: JSONObject?): IndexedZip? {
    val route = json?.optString("route").orEmpty()
    val uri = json?.optString("uri").orEmpty()
    return if (json == null || route.isBlank() || uri.isBlank()) {
        null
    } else {
        IndexedZip(
            route,
            CopyDocument(
                uri,
                json.optString("name"),
                "application/zip",
                json.optLong("modified"),
                json.optLong("bytes", -1),
            ),
            false,
        )
    }
}
