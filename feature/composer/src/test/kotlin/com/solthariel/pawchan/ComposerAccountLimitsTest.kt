package com.solthariel.pawchan

import com.solthariel.pawchan.composer.AttachmentEditOptions
import com.solthariel.pawchan.composer.ComposerAttachment
import com.solthariel.pawchan.composer.ComposerDraftState
import com.solthariel.pawchan.composer.ReplyComposerController
import com.solthariel.pawchan.domain.model.ThreadAddress
import com.solthariel.pawchan.domain.model.ThreadId
import com.solthariel.pawchan.domain.posting.AccountUploadLimits
import com.solthariel.pawchan.domain.posting.PostingOutcome
import com.solthariel.pawchan.domain.posting.ReplyAttachment
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Base64

@OptIn(ExperimentalCoroutinesApi::class)
class ComposerAccountLimitsTest {
    private val address = ThreadAddress(defaultBoardAddress(), requireNotNull(ThreadId.parse(42)))
    private val files = List(8) { ComposerAttachment(ReplyAttachment("$it.png", "image/png", byteArrayOf(1))) }

    @Test
    fun anonymousExcessSelectionIsPreservedWhenSubmissionIsRejected() =
        runTest {
            val repository = FakePostingRepository()
            val controller = ReplyComposerController(this, address, repository, {})
            controller.open(false)
            advanceUntilIdle()
            controller.updateDraft(ComposerDraftState(attachments = files))
            assertTrue(controller.state.value.canSubmit)
            controller.submit()
            advanceUntilIdle()
            assertNull(repository.lastDraft)
            assertEquals(files, controller.state.value.attachments)
            assertNotNull(controller.state.value.rejectionReason)
        }

    @Test
    fun verifiedEightFileLimitsReachPreparationAndExpireOnReload() =
        runTest {
            val repository = FakePostingRepository()
            repository.accountLimits = AccountUploadLimits(8, ReplyAttachment.MAX_ATTACHMENT_BYTES)
            val controller =
                ReplyComposerController(this, address, repository, {}, prepareFiles = { selected, rules ->
                    assertEquals(8, rules.maxFiles)
                    assertTrue(rules.accepts(selected.map { it.source }))
                    PostingOutcome.Success(selected.map { it.source })
                })
            controller.open(false)
            advanceUntilIdle()
            assertEquals(
                8,
                controller.state.value.rules
                    ?.maxFiles,
            )
            repository.accountLimits = null
            controller.reloadCaptcha()
            advanceUntilIdle()
            assertEquals(
                4,
                controller.state.value.rules
                    ?.maxFiles,
            )
            repository.accountLimits = AccountUploadLimits(8, ReplyAttachment.MAX_ATTACHMENT_BYTES)
            controller.reloadCaptcha()
            advanceUntilIdle()
            controller.updateDraft(ComposerDraftState(attachments = files))
            controller.submit()
            advanceUntilIdle()
            assertEquals(8, repository.lastDraft?.attachments?.size)
        }

    @Test
    fun spoilerSurvivesPreparationAndReachesPostingRepository() =
        runTest {
            val repository = FakePostingRepository()
            val controller = ReplyComposerController(this, address, repository, {})
            controller.open(false)
            advanceUntilIdle()
            val bytes =
                Base64.getDecoder().decode(
                    "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mP8/x8AAwMCAO+jRZkAAAAASUVORK5CYII=",
                )
            val selected =
                ComposerAttachment(
                    ReplyAttachment("spoiler.png", "image/png", bytes),
                    options = AttachmentEditOptions(spoiler = true),
                )
            controller.updateDraft(ComposerDraftState(attachments = listOf(selected)))
            assertTrue(controller.state.value.canSubmit)
            controller.submit()
            controller.state.first { !it.submitting }
            assertNotNull(controller.state.value.rejectionReason, repository.lastDraft)
            val uploaded = requireNotNull(repository.lastDraft).attachments.single()
            assertTrue(uploaded.spoiler)
            assertEquals(selected.source.fileName, uploaded.fileName)
            assertTrue(uploaded.content.contentEquals(selected.source.content))
            assertTrue(selected.options.spoiler)
        }
}
