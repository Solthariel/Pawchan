package com.solthariel.pawchan.downloads

import androidx.work.WorkInfo
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test

class InterruptedDownloadStatusTest {
    @Test fun `only actual cancellation is reported as cancelled`() {
        assertEquals(DownloadStatus.CANCELLED, interruptedStatus(WorkInfo.State.CANCELLED))
        assertEquals(DownloadStatus.WAITING, interruptedStatus(WorkInfo.State.ENQUEUED))
        assertEquals(DownloadStatus.WAITING, interruptedStatus(WorkInfo.State.BLOCKED))
        assertEquals(DownloadStatus.RUNNING, interruptedStatus(WorkInfo.State.RUNNING))
        assertEquals(DownloadStatus.SUCCEEDED, interruptedStatus(WorkInfo.State.SUCCEEDED))
        assertEquals(DownloadStatus.FAILED, interruptedStatus(WorkInfo.State.FAILED))
    }

    @Test fun `old running state is ignored then retry and cancellation are reported in order`() =
        runBlocking {
            val snapshots =
                flowOf(
                    DownloadWorkSnapshot(WorkInfo.State.RUNNING, 1),
                    DownloadWorkSnapshot(WorkInfo.State.ENQUEUED, 1),
                    DownloadWorkSnapshot(WorkInfo.State.RUNNING, 2),
                    DownloadWorkSnapshot(WorkInfo.State.CANCELLED, 2),
                )
            val statuses = mutableListOf<DownloadStatus>()
            watchStoppedWork(snapshots, 1) { statuses += it }
            assertEquals(listOf(DownloadStatus.WAITING, DownloadStatus.RUNNING, DownloadStatus.CANCELLED), statuses)
        }

    @Test fun `new attempt already running is not mistaken for stopped attempt`() =
        runBlocking {
            val statuses = mutableListOf<DownloadStatus>()
            watchStoppedWork(
                flowOf(
                    DownloadWorkSnapshot(WorkInfo.State.RUNNING, 2),
                    DownloadWorkSnapshot(WorkInfo.State.SUCCEEDED, 2),
                ),
                1,
            ) { statuses += it }
            assertEquals(listOf(DownloadStatus.RUNNING, DownloadStatus.SUCCEEDED), statuses)
        }
}
