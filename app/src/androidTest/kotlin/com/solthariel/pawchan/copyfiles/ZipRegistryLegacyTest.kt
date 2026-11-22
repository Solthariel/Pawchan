package com.solthariel.pawchan.copyfiles

import android.content.Context
import androidx.core.content.FileProvider
import androidx.core.content.edit
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.util.UUID

@RunWith(AndroidJUnit4::class)
class ZipRegistryLegacyTest {
    @Test fun migratesLegacyAndRetainsBothDocumentsAfterUpdatesAndReopen() =
        runBlocking {
            val context = ApplicationProvider.getApplicationContext<Context>()
            val name = "zip-registry-legacy-${UUID.randomUUID()}"
            val firstJob = SupervisorJob()
            val secondJob = SupervisorJob()
            val oldFile = zipFile(context)
            val newFile = zipFile(context)
            val oldDocument = document(context, oldFile)
            val newDocument = document(context, newFile)
            val legacy = context.getSharedPreferences(name, Context.MODE_PRIVATE)
            val oldJson = legacyEntry(oldDocument)
            try {
                legacy.edit(commit = true) { putString("documents", oldJson) }
                val first = ZipRegistry(context, CoroutineScope(firstJob + Dispatchers.IO), name)
                first.refresh()
                assertEquals("old-route", awaitFiles(first, 1).single().route)
                first.record("new-route", newDocument, persistGrant = false)
                assertTrue(awaitFiles(first, 2).all { it.available })
                first.record("updated-route", oldDocument, persistGrant = false)
                withTimeout(5_000) { first.files.first { entries -> entries.any { it.route == "updated-route" } } }
                firstJob.cancelAndJoin()
                val reopened = ZipRegistry(context, CoroutineScope(secondJob + Dispatchers.IO), name)
                reopened.refresh()
                val entries = awaitFiles(reopened, 2)
                assertEquals(setOf("updated-route", "new-route"), entries.map { it.route }.toSet())
                assertTrue(entries.all { it.available })
                assertEquals(oldJson, legacy.getString("documents", null))
            } finally {
                firstJob.cancelAndJoin()
                secondJob.cancelAndJoin()
                oldFile.delete()
                newFile.delete()
                context.deleteSharedPreferences(name)
                File(context.filesDir, "datastore/$name.preferences_pb").delete()
            }
        }

    private suspend fun awaitFiles(
        registry: ZipRegistry,
        count: Int,
    ): List<IndexedZip> = withTimeout(5_000) { registry.files.first { it.size == count } }

    private fun zipFile(context: Context): File =
        File(context.cacheDir, "media_clipboard/${UUID.randomUUID()}.zip").apply {
            parentFile?.mkdirs()
            writeText("zip test")
        }

    private fun document(
        context: Context,
        file: File,
    ): CopyDocument {
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.media", file)
        return CopyDocument(uri.toString(), file.name, "application/zip", 1, -1)
    }

    private fun legacyEntry(document: CopyDocument): String =
        JSONArray()
            .put(
                JSONObject()
                    .put("route", "old-route")
                    .put("uri", document.uri)
                    .put("name", document.name)
                    .put("modified", document.modified)
                    .put("bytes", document.bytes),
            ).toString()
}
