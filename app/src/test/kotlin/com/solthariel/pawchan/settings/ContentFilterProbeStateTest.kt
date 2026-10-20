package com.solthariel.pawchan.settings

import androidx.compose.ui.unit.dp
import com.solthariel.pawchan.navigation.defaultBoardAddress
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Test

class ContentFilterProbeStateTest {
    private val base = newContentFilterRule(defaultBoardAddress(), "кот")

    @Test fun noMatchCaptionDoesNotDisappearWhileAnotherNoMatchIsBeingCalculated() =
        runTest {
            val state = ContentFilterProbeState(StandardTestDispatcher(testScheduler))
            state.check(FilterProbeInput(base, "собака"))
            repeat(8) { count ->
                val next = FilterProbeInput(newContentFilterRule(defaultBoardAddress(), "кот$count"), "собака")
                val job = launch(start = CoroutineStart.UNDISPATCHED) { state.check(next) }
                assertEquals("Нет совпадений", state.caption(next.text))
                assertNull(state.preview(next))
                job.join()
                assertEquals("Нет совпадений", state.caption(next.text))
            }
        }

    @Test fun changedTextCannotReceiveHighlightingForThePreviousText() =
        runTest {
            val state = ContentFilterProbeState(StandardTestDispatcher(testScheduler))
            val matched = FilterProbeInput(base, "кот")
            state.check(matched)
            assertFalse(state.preview(matched)?.spanStyles.isNullOrEmpty())
            val changed = FilterProbeInput(base, "собака")
            val job = launch(start = CoroutineStart.UNDISPATCHED) { state.check(changed) }
            assertNull(state.preview(changed))
            job.join()
            assertEquals("Нет совпадений", state.caption(changed.text))
            assertEquals(emptyList<Any>(), state.preview(changed)?.spanStyles)
        }

    @Test fun clearingTheProbeInvalidatesAnOlderPendingResult() =
        runTest {
            val state = ContentFilterProbeState(StandardTestDispatcher(testScheduler))
            val pending = FilterProbeInput(base, "кот")
            val job = launch(start = CoroutineStart.UNDISPATCHED) { state.check(pending) }
            state.check(FilterProbeInput(base, ""))
            job.join()
            assertEquals("", state.caption(""))
            assertEquals("", state.caption("новый текст"))
            assertNull(state.preview(pending))
        }

    @Test fun cancellationCannotReplaceTheLastCompletedCaption() =
        runTest {
            val state = ContentFilterProbeState(StandardTestDispatcher(testScheduler))
            state.check(FilterProbeInput(base, "собака"))
            val job = launch(start = CoroutineStart.UNDISPATCHED) { state.check(FilterProbeInput(base, "кот")) }
            job.cancel()
            job.join()
            assertEquals("Нет совпадений", state.caption("кот"))
            assertNull(state.preview(FilterProbeInput(base, "кот")))
        }

    @Test fun aRealMatchReplacesTheCaptionWithoutAnIntermediateLoadingMessage() =
        runTest {
            val state = ContentFilterProbeState(StandardTestDispatcher(testScheduler))
            state.check(FilterProbeInput(base, "собака"))
            state.check(FilterProbeInput(base, "кот"))
            assertEquals("Совпало: «кот»", state.caption("кот"))
            state.check(FilterProbeInput(base, "собака"))
            assertEquals("Нет совпадений", state.caption("собака"))
        }

    @Test fun inputHasOneTouchableLineAndRespectsLargeFontsWithoutHintDependentSpace() {
        assertEquals(56.dp, filterInputHeight(24.dp))
        assertEquals(60.dp, filterInputHeight(28.dp))
        assertEquals(80.dp, filterInputHeight(48.dp))
    }
}
