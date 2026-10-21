package com.solthariel.pawchan

import com.solthariel.pawchan.composer.ComposerDraftCoordinator
import com.solthariel.pawchan.composer.ComposerDraftState
import com.solthariel.pawchan.composer.NewThreadViewModel
import com.solthariel.pawchan.composer.toStoredDraft
import com.solthariel.pawchan.domain.drafts.ComposerDraftKey
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class NewThreadDraftPersistenceTest {
    @Test fun hiddenEditorDoesNotEraseSavedDraftAndReopensAfterClosing() =
        runTest {
            val dispatcher = StandardTestDispatcher(testScheduler)
            Dispatchers.setMain(dispatcher)
            try {
                val disk = TestComposerDraftRepository()
                val board = defaultBoardAddress()
                val key = ComposerDraftKey(board)
                disk.saved[key] = ComposerDraftState(comment = "remembered").toStoredDraft()
                val storage = ComposerDraftCoordinator(this, disk, { testScheduler.currentTime }, dispatcher)
                val model =
                    NewThreadViewModel(
                        board,
                        FakePostingRepository(),
                        drafts = storage,
                        autoOpen = false,
                        preparationDispatcher = Dispatchers.Unconfined,
                    )
                model.saveDraftNow()
                advanceUntilIdle()
                assertEquals("remembered", disk.saved[key]?.text?.comment)
                model.open()
                advanceUntilIdle()
                assertEquals("remembered", model.state.value.comment)
                model.updateDraft(
                    model.state.value.draft
                        .copy(comment = "updated"),
                )
                var closed = false
                model.close { closed = true }
                advanceUntilIdle()
                assertEquals(true, closed)
                assertFalse(model.state.value.draft.filesLoading)
                assertEquals("", model.state.value.comment)
                model.saveDraftNow()
                advanceUntilIdle()
                assertEquals("updated", disk.saved[key]?.text?.comment)
                model.open()
                advanceUntilIdle()
                assertEquals("updated", model.state.value.comment)
            } finally {
                Dispatchers.resetMain()
            }
        }
}
