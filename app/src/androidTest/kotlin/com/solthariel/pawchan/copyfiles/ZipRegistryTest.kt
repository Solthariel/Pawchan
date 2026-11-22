package com.solthariel.pawchan.copyfiles

import android.content.Context
import androidx.core.content.FileProvider
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.util.UUID

@RunWith(AndroidJUnit4::class)
class ZipRegistryTest {
    @Test fun recordsOnlyActualAvailableFilesAndDetectsDeletionAfterReload() =
        runBlocking {
            val context = ApplicationProvider.getApplicationContext<Context>()
            val prefs = "test-zips-${UUID.randomUUID()}"
            val firstJob = SupervisorJob()
            val secondJob = SupervisorJob()
            val scope = CoroutineScope(firstJob + Dispatchers.IO)
            val file = File(context.cacheDir, "media_clipboard/${UUID.randomUUID()}.zip")
            file.parentFile?.mkdirs()
            file.writeText("zip test")
            val uri = FileProvider.getUriForFile(context, "${context.packageName}.media", file)
            val document = CopyDocument(uri.toString(), file.name, "application/zip", 1, -1)
            val route = "thread/dvach/b/12"
            try {
                val registry = ZipRegistry(context, scope, prefs)
                registry.record(route, document, persistGrant = false)
                val available = withTimeout(5_000) { registry.files.first { it.any { entry -> entry.available } } }
                assertEquals(route, available.single().route)
                assertTrue(available.single().available)
                file.delete()
                firstJob.cancelAndJoin()
                val reloaded = ZipRegistry(context, CoroutineScope(secondJob + Dispatchers.IO), prefs)
                reloaded.refresh()
                val missing = withTimeout(5_000) { reloaded.files.first { it.isNotEmpty() } }
                assertEquals(document.uri, missing.single().document.uri)
                assertFalse(missing.single().available)
            } finally {
                firstJob.cancelAndJoin()
                secondJob.cancelAndJoin()
                file.delete()
                File(context.filesDir, "datastore/$prefs.preferences_pb").delete()
            }
        }
}
