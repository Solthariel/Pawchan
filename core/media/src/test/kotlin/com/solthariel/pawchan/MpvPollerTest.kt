package com.solthariel.pawchan

import com.solthariel.pawchan.media.video.MpvPollHost
import com.solthariel.pawchan.media.video.MpvPollSample
import com.solthariel.pawchan.media.video.MpvPollTiming
import com.solthariel.pawchan.media.video.MpvPoller
import com.solthariel.pawchan.media.video.mpvFrameCounters
import org.junit.Assert.assertEquals
import org.junit.Test

class MpvPollerTest {
    @Test
    fun decoderAndOutputDropsUseDistinctNativePropertiesAndUnknownIsNotZero() {
        val sample = MpvPollSample(MpvPollTiming(400, 4000), true, false, false, null, "no", emptyMap())
        val native = mapOf("decoder-frame-drop-count" to "0", "frame-drop-count" to "7")
        val measured = mpvFrameCounters(sample, native::get)
        assertEquals(0L, measured.decoderDrops)
        assertEquals(7L, measured.outputDrops)
        assertEquals(false, measured.hardware)
        for (invalid in listOf(null, "unavailable", "-1")) {
            assertEquals(null, mpvFrameCounters(sample) { invalid }.outputDrops)
        }
    }

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
