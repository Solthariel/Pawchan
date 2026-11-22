package com.solthariel.pawchan

import com.solthariel.pawchan.media.video.MpvPollHost
import com.solthariel.pawchan.media.video.MpvPollSample
import com.solthariel.pawchan.media.video.MpvPoller
import org.junit.Assert.assertEquals
import org.junit.Test

class MpvPollerTest {
    @Test
    fun staleGenerationDoesNotReadOrReschedule() {
        val host = FakePollHost()
        val poller = MpvPoller(host)
        poller.schedule(1)
        host.token = 2
        host.runScheduled()
        assertEquals(0, host.scheduled.size)
        assertEquals(0, host.callbacks)
    }

    @Test
    fun closedOrDestroyedEngineDoesNotReadOrReschedule() {
        for ((closed, handle) in listOf(true to 1L, false to 0L)) {
            val host =
                FakePollHost().apply {
                    this.closed = closed
                    this.handle = handle
                }
            MpvPoller(host).schedule(1)
            host.runScheduled()
            assertEquals(0, host.scheduled.size)
            assertEquals(0, host.callbacks)
        }
    }
}

private class FakePollHost : MpvPollHost {
    override var token = 1
    override var closed = false
    override var handle = 1L
    override var facts: Map<String, String> = emptyMap()
    val scheduled = mutableListOf<() -> Unit>()
    var callbacks = 0

    override fun scheduleWorker(
        token: Int,
        action: () -> Unit,
    ) {
        scheduled.add(action)
    }

    fun runScheduled() {
        scheduled.removeAt(0).invoke()
    }

    override fun onMain(action: () -> Unit) {
        callbacks++
    }

    override fun onSample(sample: MpvPollSample) {
        callbacks++
    }

    override fun onEnd(reason: String?) {
        callbacks++
    }

    override fun onPlaybackRestart() {
        callbacks++
    }
}
