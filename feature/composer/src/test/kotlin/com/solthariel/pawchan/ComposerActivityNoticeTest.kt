package com.solthariel.pawchan

import com.solthariel.pawchan.composer.COMPOSER_PROGRESS_DELAY_MS
import com.solthariel.pawchan.composer.ComposerDraftActivity
import com.solthariel.pawchan.composer.delayedComposerActivityMessage
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ComposerActivityNoticeTest {
    @Test fun slowRestorationHasItsOwnMessageOnlyAfterTheDelay() =
        runTest {
            val result = async { delayedComposerActivityMessage(ComposerDraftActivity.RESTORING) }
            advanceTimeBy(COMPOSER_PROGRESS_DELAY_MS - 1)
            assertFalse(result.isCompleted)
            advanceTimeBy(1)
            runCurrent()
            assertEquals("Восстановление черновика…", result.await())
        }

    @Test fun realImportHasAnAttachmentMessage() =
        runTest {
            assertEquals("Чтение вложений…", delayedComposerActivityMessage(ComposerDraftActivity.LOADING_FILES))
        }

    @Test fun fastWorkCancelsWithoutPublishingALateMessage() =
        runTest {
            val result = async { delayedComposerActivityMessage(ComposerDraftActivity.LOADING_FILES) }
            advanceTimeBy(100)
            result.cancel()
            advanceTimeBy(COMPOSER_PROGRESS_DELAY_MS)
            assertTrue(result.isCancelled)
        }

    @Test fun errorsClosingAndIdleDoNotPretendToBeLoading() =
        runTest {
            val activities =
                listOf(
                    ComposerDraftActivity.IDLE,
                    ComposerDraftActivity.CLOSING,
                    ComposerDraftActivity.RESTORE_FAILED,
                )
            for (activity in activities) {
                assertNull(delayedComposerActivityMessage(activity))
            }
        }
}
