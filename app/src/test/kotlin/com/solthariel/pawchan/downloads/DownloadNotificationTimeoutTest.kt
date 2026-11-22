package com.solthariel.pawchan.downloads

import org.junit.Assert.assertEquals
import org.junit.Test

class DownloadNotificationTimeoutTest {
    @Test fun onlySuccessExpiresAutomatically() {
        DownloadStatus.entries.forEach { status ->
            assertEquals(if (status == DownloadStatus.SUCCEEDED) 5_000L else 0L, notificationTimeoutMillis(status))
        }
    }
}
