package com.solthariel.pawchan.downloads

import androidx.work.WorkInfo
import org.junit.Assert.assertEquals
import org.junit.Test

class DownloadStatusTest {
    private val record =
        DownloadRecord(
            "id",
            "https://example.test/a",
            "content://file",
            "a",
            DownloadOrigin(null, null),
            System.currentTimeMillis(),
            DownloadStatus.RUNNING,
        )

    @Test fun `work completion determines actual terminal state`() {
        assertEquals(DownloadStatus.SUCCEEDED, resolvedStatus(record, WorkInfo.State.SUCCEEDED, true))
        assertEquals(DownloadStatus.FAILED, resolvedStatus(record, WorkInfo.State.FAILED, true))
        assertEquals(DownloadStatus.CANCELLED, resolvedStatus(record, WorkInfo.State.CANCELLED, true))
    }

    @Test fun `no success is claimed while work is pending`() {
        assertEquals(DownloadStatus.RUNNING, resolvedStatus(record, WorkInfo.State.RUNNING, true))
        assertEquals(DownloadStatus.QUEUED, resolvedStatus(record, WorkInfo.State.ENQUEUED, true))
        assertEquals(DownloadStatus.RUNNING, resolvedStatus(record, null, false))
        assertEquals(DownloadStatus.FAILED, resolvedStatus(record.copy(created = 0), null, true))
    }

    @Test fun `indexed work states match records by id rather than list order`() {
        val second = record.copy(id = "second")
        val resolved =
            resolveDownloadStates(
                listOf(record, second),
                mapOf("second" to WorkInfo.State.CANCELLED, "id" to WorkInfo.State.SUCCEEDED),
            )
        assertEquals(listOf(DownloadStatus.SUCCEEDED, DownloadStatus.CANCELLED), resolved.map { it.status })
    }

    @Test fun `not-yet-loaded work state preserves running download`() {
        assertEquals(listOf(record), resolveDownloadStates(listOf(record), null))
    }

    @Test fun `prepared downloads filter by name without losing reconciliation input`() {
        val second = record.copy(id = "second", name = "other.jpg")
        val prepared = prepareDownloads(listOf(record, second), null, "OTHER")
        assertEquals(listOf(record, second), prepared.source)
        assertEquals(listOf(second), prepared.visible)
        assertEquals(listOf(record, second), prepared.resolved)
    }

    @Test fun `finished persisted work survives pruning`() {
        val done = record.copy(status = DownloadStatus.SUCCEEDED)
        assertEquals(DownloadStatus.SUCCEEDED, resolvedStatus(done, null, true))
        assertEquals(DownloadStatus.FAILED, resolvedStatus(done, WorkInfo.State.FAILED, true))
    }
}
