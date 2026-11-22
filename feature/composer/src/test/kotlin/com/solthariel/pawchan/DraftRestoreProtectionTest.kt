package com.solthariel.pawchan

import com.solthariel.pawchan.composer.ComposerDraftCoordinator
import com.solthariel.pawchan.composer.ComposerDraftState
import com.solthariel.pawchan.composer.ReplyComposerController
import com.solthariel.pawchan.composer.toStoredDraft
import com.solthariel.pawchan.domain.drafts.ComposerDraftKey
import com.solthariel.pawchan.domain.model.ThreadAddress
import com.solthariel.pawchan.domain.model.ThreadId
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class DraftRestoreProtectionTest {
    @Test
    fun editAfterFailedRestoreDoesNotOverwriteSavedDraft() =
        runTest {
            val board = defaultBoardAddress()
            val address = ThreadAddress(board, requireNotNull(ThreadId.parse(42)))
            val key = ComposerDraftKey(board, address.thread)
            val disk = TestComposerDraftRepository()
            disk.saved[key] = ComposerDraftState(comment = "original").toStoredDraft()
            disk.failReads = true
            val storage =
                ComposerDraftCoordinator(
                    this,
                    disk,
                    { testScheduler.currentTime },
                    StandardTestDispatcher(testScheduler),
                )
            val controller =
                ReplyComposerController(
                    this,
                    address,
                    FakePostingRepository(),
                    {},
                    drafts = storage,
                    preparationDispatcher = Dispatchers.Unconfined,
                )
            controller.open(false)
            advanceUntilIdle()
            controller.updateDraft(
                controller.state.value.draft
                    .copy(comment = "new text"),
            )
            advanceUntilIdle()
            assertEquals("original", disk.saved[key]?.text?.comment)
        }
}
