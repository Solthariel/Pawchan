package com.solthariel.pawchan.update

import androidx.lifecycle.ViewModelStore
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.Assert.assertEquals
import org.junit.Test
import java.io.File

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class UpdateAccessChangesTest {
    @Test fun changingAccessCancelsThePendingCheckAndDiscardsTheOldOffer() =
        runTest {
            Dispatchers.setMain(StandardTestDispatcher(testScheduler))
            val result = CompletableDeferred<Pair<UpdatePackage, String?>?>()
            val client = AccessChangeOperations(result)
            val model = UpdateViewModel(client)
            val store = ViewModelStore().apply { put("updates", model) }
            try {
                model.check(UpdateConfiguration("https://example.org", 1, "0.1.0"))
                runCurrent()
                model.accessChanged()
                result.complete(UpdatePackage().apply { versionCode = 2 } to null)
                runCurrent()
                assertEquals(UpdateUiState.Idle, model.state.value)
            } finally {
                store.clear()
                Dispatchers.resetMain()
            }
        }
}

private class AccessChangeOperations(
    private val result: CompletableDeferred<Pair<UpdatePackage, String?>?>,
) : UpdateOperations {
    override suspend fun check(): Pair<UpdatePackage, String?>? = result.await()

    override suspend fun download(
        release: UpdatePackage,
        onProgress: (Int) -> Unit,
    ): File = error("Not requested")

    override fun close() = Unit
}
