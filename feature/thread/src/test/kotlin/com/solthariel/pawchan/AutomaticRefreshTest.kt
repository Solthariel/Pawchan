package com.solthariel.pawchan

import com.solthariel.pawchan.thread.watch.ThreadWatcherController
import com.solthariel.pawchan.thread.watch.ThreadWatcherPoll
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class AutomaticRefreshTest {
    @Test fun automaticPollingRespectsVisibilityWithoutPersistedSubscription() =
        runTest {
            var calls = 0
            val watcher =
                ThreadWatcherController(this) {
                    calls++
                    ThreadWatcherPoll(0)
                }
            watcher.configureAutomatic(true, 15_000L)
            advanceTimeBy(15_000L)
            runCurrent()
            assertEquals(0, calls)
            watcher.setActive(true)
            advanceTimeBy(15_000L)
            runCurrent()
            assertEquals(1, calls)
            watcher.setActive(false)
            advanceTimeBy(60_000L)
            runCurrent()
            assertEquals(1, calls)
            watcher.configureAutomatic(false, 30_000L)
            watcher.setActive(true)
            advanceTimeBy(60_000L)
            runCurrent()
            assertEquals(1, calls)
            watcher.setActive(false)
        }
}
