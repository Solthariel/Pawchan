package com.solthariel.pawchan

import androidx.work.ListenableWorker
import com.solthariel.pawchan.downloads.DownloadStatus
import org.junit.Assert.assertEquals
import org.junit.Test

class MediaDownloadAttemptTest {
    @Test
    fun attemptsMapToTheirPublicWorkResultsAndHistoryStates() {
        assertEquals(ListenableWorker.Result.success(), MediaDownloadAttempt.SUCCESS.workResult())
        assertEquals(DownloadStatus.SUCCEEDED, MediaDownloadAttempt.SUCCESS.status)
        assertEquals(ListenableWorker.Result.retry(), MediaDownloadAttempt.RETRY.workResult())
        assertEquals(DownloadStatus.WAITING, MediaDownloadAttempt.RETRY.status)
        assertEquals(ListenableWorker.Result.failure(), MediaDownloadAttempt.FAILURE.workResult())
        assertEquals(DownloadStatus.FAILED, MediaDownloadAttempt.FAILURE.status)
    }
}
