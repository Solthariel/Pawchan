package com.solthariel.pawchan

import com.solthariel.pawchan.media.video.VideoPrivacyAccess
import com.solthariel.pawchan.media.video.VideoSessionPrivacy
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class VideoSessionPrivacyTest {
    @Test
    fun lockStillPausesAParkedSessionWithoutAnyActivityCollector() =
        runTest {
            var pauses = 0
            val access = TestAccess()
            val privacy = VideoSessionPrivacy(backgroundScope) { pauses += 1 }
            privacy.bind(access)
            runCurrent()
            assertEquals(0, pauses)
            access.changes.value = false
            runCurrent()
            assertEquals(1, pauses)
            access.changes.value = true
            runCurrent()
            assertEquals(1, pauses)
            privacy.close()
        }

    @Test
    fun deniedPermissionPausesSynchronouslyBeforeLaunchingACollector() =
        runTest {
            var pauses = 0
            val access = TestAccess()
            access.changes.value = false
            val privacy = VideoSessionPrivacy(backgroundScope) { pauses += 1 }
            privacy.bind(access)
            assertEquals(1, pauses)
            privacy.close()
        }

    @Test
    fun closeCancelsPrivacyObservation() =
        runTest {
            var pauses = 0
            val access = TestAccess()
            val privacy = VideoSessionPrivacy(backgroundScope) { pauses += 1 }
            privacy.bind(access)
            runCurrent()
            privacy.close()
            access.changes.value = false
            runCurrent()
            assertEquals(0, pauses)
        }
}

private class TestAccess : VideoPrivacyAccess {
    override val changes = MutableStateFlow(true)

    override fun isAllowed() = changes.value
}
