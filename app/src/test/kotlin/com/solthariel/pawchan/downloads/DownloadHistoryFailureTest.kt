package com.solthariel.pawchan.downloads

import org.junit.Assert.assertEquals
import org.junit.Test

class DownloadHistoryFailureTest {
    private val record =
        DownloadRecord(
            "1",
            "https://example.test/a",
            "content://file/1",
            "a",
            DownloadOrigin(null, null),
            1,
            DownloadStatus.SUCCEEDED,
        )

    @Test fun failedRefreshKeepsRecordsAndSignalsError() {
        val content = DownloadHistoryState.Content(listOf(record))
        assertEquals(content.copy(refreshFailures = 1), downloadHistoryReadFailure(content))
    }

    @Test fun repeatedFailuresRemainDistinct() {
        val content = DownloadHistoryState.Content(listOf(record), refreshFailures = 1)
        assertEquals(content.copy(refreshFailures = 2), downloadHistoryReadFailure(content))
    }

    @Test fun failedInitialOrEmptyLoadShowsError() {
        assertEquals(DownloadHistoryState.Error, downloadHistoryReadFailure(null))
        assertEquals(DownloadHistoryState.Error, downloadHistoryReadFailure(DownloadHistoryState.Content(emptyList())))
    }
}
