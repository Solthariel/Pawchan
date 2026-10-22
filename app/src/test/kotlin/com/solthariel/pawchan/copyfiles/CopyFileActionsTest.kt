package com.solthariel.pawchan.copyfiles

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.io.OutputStream

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class CopyFileActionsTest {
    @Test fun successfulWriteOnlyPublishesResultAfterWriterFinishes() =
        runTest {
            val store = RecordingCopyStore()
            val actions = CopyFileActions(store, "content://tree/1", CopyFileKind.BACKUP, this)
            val done = CompletableDeferred<Unit>()
            actions.save("a.pawbackup") {
                done.await()
                it.write(42)
            }
            runCurrent()
            assertTrue(actions.busy)
            assertNull(actions.saved)
            done.complete(Unit)
            runCurrent()
            assertFalse(actions.busy)
            assertEquals("a.pawbackup", actions.saved?.name)
            assertEquals(listOf(42.toByte()), store.bytes.toList())
        }

    @Test fun failedSaveDoesNotPublishSuccessAndExplicitRetryWorks() =
        runTest {
            val store = RecordingCopyStore().apply { failure = IOException("full") }
            val actions = CopyFileActions(store, "tree", CopyFileKind.THREAD, this)
            actions.save("a.zip") { it.write(1) }
            runCurrent()
            assertNull(actions.saved)
            assertEquals("full", actions.error)
            assertFalse(actions.busy)
            store.failure = null
            actions.save("a.zip") { it.write(2) }
            runCurrent()
            assertNotNull(actions.saved)
            assertNull(actions.error)
        }

    @Test fun missingGrantIsRecoverableInsteadOfShowingSaved() =
        runTest {
            val store = RecordingCopyStore().apply { failure = SecurityException() }
            val actions = CopyFileActions(store, "tree", CopyFileKind.BACKUP, this)
            actions.browse()
            runCurrent()
            assertTrue(actions.browsing)
            assertTrue(actions.documents.isEmpty())
            assertNotNull(actions.error)
            assertFalse(actions.busy)
        }

    @Test fun concurrentSaveClicksCannotStartDuplicateWrites() =
        runTest {
            val store = RecordingCopyStore()
            val actions = CopyFileActions(store, "tree", CopyFileKind.THREAD, this)
            val done = CompletableDeferred<Unit>()
            actions.save("a.zip") { done.await() }
            actions.save("b.zip") { error("duplicate") }
            runCurrent()
            assertEquals(1, store.writes)
            done.complete(Unit)
            runCurrent()
            assertEquals("a.zip", actions.saved?.name)
        }

    @Test fun cancellationReleasesBusyWithoutInventingAResult() =
        runTest {
            val store = RecordingCopyStore()
            val child =
                launch {
                    val actions = CopyFileActions(store, "tree", CopyFileKind.THREAD, this)
                    actions.save("a.zip") { awaitCancellation() }
                    runCurrent()
                    assertTrue(actions.busy)
                }
            runCurrent()
            child.cancel()
            child.join()
            assertEquals(1, store.writes)
        }

    @Test fun listingAndClosingDoNotMutateTheSavedFiles() =
        runTest {
            val store = RecordingCopyStore()
            val actions = CopyFileActions(store, "tree", CopyFileKind.THREAD, this)
            actions.browse()
            runCurrent()
            assertEquals(listOf(store.document), actions.documents)
            actions.closeList()
            assertFalse(actions.browsing)
            assertEquals(0, store.writes)
        }
}

private class RecordingCopyStore : CopyFileStore {
    var failure: Exception? = null
    var writes = 0
    var bytes = byteArrayOf()
    val document = CopyDocument("content://test/1", "a.zip", "application/zip", 1, 1)

    override suspend fun write(
        tree: String,
        kind: CopyFileKind,
        name: String,
        writer: suspend (OutputStream) -> Unit,
    ): CopyDocument {
        writes++
        failure?.let { throw it }
        val stream = ByteArrayOutputStream()
        writer(stream)
        bytes = stream.toByteArray()
        return document.copy(name = name)
    }

    override suspend fun list(
        tree: String,
        kind: CopyFileKind,
    ): List<CopyDocument> {
        failure?.let { throw it }
        return listOf(document)
    }
}
