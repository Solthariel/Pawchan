package com.solthariel.pawchan

import com.solthariel.pawchan.composer.COMPOSER_SUCCESS_DISPLAY_MILLIS
import com.solthariel.pawchan.composer.ComposerDraftState
import com.solthariel.pawchan.composer.ReplyComposerController
import com.solthariel.pawchan.domain.model.ThreadAddress
import com.solthariel.pawchan.domain.model.ThreadId
import com.solthariel.pawchan.domain.posting.PostingOutcome
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ReplySuccessStatusTest {
    @Test
    fun showsSuccessInTheExistingFormBeforeClosingAndPreservesItsFields() =
        runTest {
            var posted = 0
            val repository = FakePostingRepository()
            val address = ThreadAddress(defaultBoardAddress(), requireNotNull(ThreadId.parse(42)))
            val controller =
                ReplyComposerController(this, address, repository, { posted++ }, prepareFiles = { files, _ ->
                    PostingOutcome.Success(files.map { it.source })
                })
            controller.open(false)
            advanceUntilIdle()
            val draft = ComposerDraftState(comment = "Ответ", email = "mail")
            controller.updateDraft(draft)
            controller.submit()
            runCurrent()
            assertEquals(1, posted)
            assertEquals(true, controller.state.value.visible)
            assertEquals(true, controller.state.value.progress.sent)
            assertEquals(false, controller.state.value.canSubmit)
            assertEquals(draft, controller.state.value.draft)
            controller.reloadCaptcha()
            controller.clearAll()
            controller.submit()
            assertEquals(1, posted)
            assertEquals(draft, controller.state.value.draft)
            advanceTimeBy(COMPOSER_SUCCESS_DISPLAY_MILLIS)
            runCurrent()
            assertEquals(false, controller.state.value.visible)
            assertEquals("", controller.state.value.comment)
        }
}
