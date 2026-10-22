package com.solthariel.pawchan.update

import androidx.lifecycle.ViewModelStore
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.io.IOException

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class UpdateViewModelTest {
    @Test fun repeatedCheckStartsOnlyOneOperation() =
        scenario { model, client ->
            model.check(configuration)
            model.check(configuration)
            runCurrent()
            assertEquals(1, client.checks)
            assertSame(UpdateUiState.Checking, model.state.value)
        }

    @Test fun rapidDownloadClicksAndChecksCannotRaceForOneFile() =
        scenario { model, client ->
            available(model, client)
            model.download(client.release)
            model.download(client.release)
            model.check(configuration)
            assertTrue(model.state.value is UpdateUiState.Downloading)
            runCurrent()
            assertEquals(1, client.downloads)
            assertEquals(1, client.checks)
        }

    @Test fun downloadRequiresTheCurrentlyOfferedRelease() =
        scenario { model, client ->
            model.download(client.release)
            runCurrent()
            assertEquals(0, client.downloads)
            available(model, client)
            model.download(UpdatePackage().apply { versionCode = client.release.versionCode })
            runCurrent()
            assertEquals(0, client.downloads)
            assertTrue(model.state.value is UpdateUiState.Available)
        }

    @Test fun successfulDownloadPublishesOnlyTheCompletedFile() =
        scenario { model, client ->
            available(model, client)
            model.download(client.release)
            runCurrent()
            client.downloadResult.complete(File("verified.apk"))
            runCurrent()
            assertEquals(File("verified.apk").absolutePath, (model.state.value as UpdateUiState.Ready).apkPath)
        }

    @Test fun failedDownloadCanBeCheckedAndRetried() =
        scenario { model, client ->
            available(model, client)
            model.download(client.release)
            runCurrent()
            client.downloadResult.completeExceptionally(IOException("Network lost"))
            runCurrent()
            assertEquals(UpdateUiState.Error(UpdateError.GENERAL), model.state.value)
            model.check(configuration)
            runCurrent()
            assertEquals(2, client.checks)
            assertTrue(model.state.value is UpdateUiState.Available)
        }

    @Test fun clearingOwnerCancelsOperationWithoutPublishingAnError() =
        scenario { model, client ->
            model.check(configuration)
            runCurrent()
            val store = ViewModelStore().apply { put("updates", model) }
            store.clear()
            runCurrent()
            client.checkResult.complete(client.release to null)
            runCurrent()
            assertTrue(client.closed)
            assertSame(UpdateUiState.Checking, model.state.value)
        }

    @Test fun blankManifestDoesNotStartHttp() =
        scenario { model, client ->
            model.check(configuration.copy(manifestUrl = ""))
            runCurrent()
            assertEquals(0, client.checks)
            assertEquals(UpdateUiState.Error(UpdateError.NOT_CONFIGURED), model.state.value)
        }

    private fun scenario(test: suspend TestScope.(UpdateViewModel, PendingUpdateOperations) -> Unit) =
        runTest {
            Dispatchers.setMain(StandardTestDispatcher(testScheduler))
            val client = PendingUpdateOperations()
            val model = UpdateViewModel(client)
            val store = ViewModelStore().apply { put("updates", model) }
            try {
                test(model, client)
            } finally {
                store.clear()
                Dispatchers.resetMain()
            }
        }

    private fun TestScope.available(
        model: UpdateViewModel,
        client: PendingUpdateOperations,
    ) {
        model.check(configuration)
        client.checkResult.complete(client.release to null)
        runCurrent()
    }

    private val configuration = UpdateConfiguration("https://example.org/feed.json", 27, "0.1.26")
}

private class PendingUpdateOperations : UpdateOperations {
    val release =
        UpdatePackage().apply {
            versionCode = 28
            versionName = "0.1.27"
        }
    val checkResult = CompletableDeferred<Pair<UpdatePackage, String?>?>()
    val downloadResult = CompletableDeferred<File>()
    var checks = 0
    var downloads = 0
    var closed = false

    override suspend fun check(): Pair<UpdatePackage, String?>? {
        checks++
        return checkResult.await()
    }

    override suspend fun download(
        release: UpdatePackage,
        onProgress: (Int) -> Unit,
    ): File {
        downloads++
        onProgress(50)
        return downloadResult.await()
    }

    override fun close() {
        closed = true
    }
}
