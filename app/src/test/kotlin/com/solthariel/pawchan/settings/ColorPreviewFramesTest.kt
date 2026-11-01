package com.solthariel.pawchan.settings

import androidx.compose.material3.lightColorScheme
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class ColorPreviewFramesTest {
    @Test fun busyInputPublishesFramesAndKeepsOnlyNewestPendingRequest() =
        runTest {
            val input = Channel<ColorPreviewRequest?>()
            val started = Channel<ColorPreviewRequest>(Channel.UNLIMITED)
            val release = Channel<Unit>()
            val frames = Channel<ColorPreviewFrame>(Channel.UNLIMITED)
            val job =
                launch {
                    input
                        .receiveAsFlow()
                        .colorPreviewFrames {
                            started.send(it)
                            release.receive()
                            lightColorScheme()
                        }.collect { frames.send(it) }
                }
            val first = request(1)
            input.send(first)
            assertEquals(first, started.receive())
            repeat(PENDING_UPDATES) { input.send(request(it + 2)) }
            // Rendezvous sends bound input; the worker retains just the latest pending request.
            input.close()
            release.send(Unit)
            assertEquals(first, frames.receive().request)
            val last = request(PENDING_UPDATES + 1)
            assertEquals(last, started.receive())
            release.send(Unit)
            job.join()
            assertEquals(last, frames.receive().request)
            assertTrueEmpty(started)
            assertTrueEmpty(frames)
        }

    @Test fun duplicateRequestsDoNotRegenerateAndInvalidHexDoesNotClearFrame() =
        runTest {
            val input = Channel<ColorPreviewRequest?>()
            val frames = Channel<ColorPreviewFrame>(Channel.UNLIMITED)
            val colors = lightColorScheme()
            val job =
                launch {
                    input.receiveAsFlow().colorPreviewFrames { colors }.collect { frames.send(it) }
                }
            val request = request(1)
            input.send(request)
            assertEquals(ColorPreviewFrame(request, colors), frames.receive())
            input.send(request)
            input.send(null)
            input.close()
            job.join()
            assertTrueEmpty(frames)
        }

    @Test fun closingOwnerCancelsActiveCalculationWithoutPublishing() =
        runTest {
            val input = Channel<ColorPreviewRequest?>()
            val started = Channel<Unit>()
            val release = Channel<Unit>()
            var published = false
            var cancelled = false
            val job =
                launch {
                    input
                        .receiveAsFlow()
                        .colorPreviewFrames {
                            started.send(Unit)
                            try {
                                release.receive()
                            } finally {
                                cancelled = true
                            }
                            lightColorScheme()
                        }.collect { published = true }
                }
            input.send(request(1))
            started.receive()
            job.cancelAndJoin()
            assertEquals(true, cancelled)
            assertFalse(published)
        }

    @Test fun modeAndBlackOverrideArePartOfFrameIdentity() {
        val seed = 1
        val requests =
            listOf(
                ColorPreviewRequest(seed, false, false),
                ColorPreviewRequest(seed, true, false),
                ColorPreviewRequest(seed, true, true),
            )
        assertEquals(requests.size, requests.distinct().size)
    }

    private fun request(seed: Int): ColorPreviewRequest = ColorPreviewRequest(seed, dark = true, pureBlack = false)

    private fun <T> assertTrueEmpty(channel: Channel<T>) {
        assertFalse("Only one calculation and one pending input are allowed", channel.tryReceive().isSuccess)
    }
}

private const val PENDING_UPDATES = 100
