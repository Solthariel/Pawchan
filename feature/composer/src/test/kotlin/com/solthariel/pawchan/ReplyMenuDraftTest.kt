package com.solthariel.pawchan

import com.solthariel.pawchan.composer.ComposerDraftCoordinator
import com.solthariel.pawchan.composer.ComposerDraftState
import com.solthariel.pawchan.composer.ReplyComposerController
import com.solthariel.pawchan.composer.toStoredDraft
import com.solthariel.pawchan.domain.drafts.ComposerDraftKey
import com.solthariel.pawchan.domain.model.ThreadAddress
import com.solthariel.pawchan.domain.model.ThreadId
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ReplyMenuDraftTest {
    @Test
    fun menuReplyWaitsForRestoreAndAppendsRatherThanOverwritingDraft() =
        runTest {
            val address = ThreadAddress(defaultBoardAddress(), requireNotNull(ThreadId.parse(42)))
            val disk = TestComposerDraftRepository()
            val key = ComposerDraftKey(address.board, address.thread)
            disk.saved[key] = ComposerDraftState(comment = "Сохранённый", email = "mail").toStoredDraft()
            val coordinator =
                ComposerDraftCoordinator(
                    this,
                    disk,
                    { testScheduler.currentTime },
                    StandardTestDispatcher(testScheduler),
                )
            val controller = ReplyComposerController(this, address, FakePostingRepository(), {}, drafts = coordinator)
            controller.open(false, ">>7\n")
            advanceUntilIdle()
            assertEquals("Сохранённый\n>>7\n", controller.state.value.comment)
            assertEquals("mail", controller.state.value.draft.email)
            controller.open(false, ">>8\n")
            advanceUntilIdle()
            assertEquals("Сохранённый\n>>7\n>>8\n", controller.state.value.comment)
        }

    @Test
    fun closingDuringRestoreDoesNotReopenOrInsertIntoAnotherForm() =
        runTest {
            val address = ThreadAddress(defaultBoardAddress(), requireNotNull(ThreadId.parse(42)))
            val controller = ReplyComposerController(this, address, FakePostingRepository(), {})
            controller.open(false, ">>7\n")
            controller.close()
            advanceUntilIdle()
            assertEquals(false, controller.state.value.visible)
            assertEquals("", controller.state.value.comment)
        }
}
