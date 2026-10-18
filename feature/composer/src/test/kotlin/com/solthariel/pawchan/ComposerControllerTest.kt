package com.solthariel.pawchan

import com.solthariel.pawchan.composer.AttachmentEditOptions
import com.solthariel.pawchan.composer.ComposerAttachment
import com.solthariel.pawchan.composer.ComposerDraftState
import com.solthariel.pawchan.composer.ReplyComposerController
import com.solthariel.pawchan.domain.model.ThreadAddress
import com.solthariel.pawchan.domain.model.ThreadId
import com.solthariel.pawchan.domain.posting.PostingFeatures
import com.solthariel.pawchan.domain.posting.PostingIdentityRules
import com.solthariel.pawchan.domain.posting.PostingOptions
import com.solthariel.pawchan.domain.posting.PostingOutcome
import com.solthariel.pawchan.domain.posting.PostingRules
import com.solthariel.pawchan.domain.posting.ReplyAttachment
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ComposerControllerTest {
    private val address = ThreadAddress(defaultBoardAddress(), requireNotNull(ThreadId.parse(42)))
    private val files =
        listOf(
            ComposerAttachment(ReplyAttachment("one.png", "image/png", byteArrayOf(1))),
            ComposerAttachment(ReplyAttachment("two.png", "image/png", byteArrayOf(2))),
        )

    @Test
    fun replySendsEmailAndAllFiles() =
        runTest {
            val repository = FakePostingRepository()
            val controller =
                ReplyComposerController(this, address, repository, {}, prepareFiles = { selected, _ ->
                    PostingOutcome.Success(selected.map { it.source })
                })
            controller.open(false)
            advanceUntilIdle()
            controller.updateDraft(ComposerDraftState(comment = "[b]reply[/b]", email = "sage", attachments = files))
            assertTrue(controller.state.value.canSubmit)
            controller.submit()
            advanceUntilIdle()
            assertEquals("sage", repository.lastDraft?.email)
            assertEquals(listOf("one.png", "two.png"), repository.lastDraft?.attachments?.map { it.fileName })
            assertFalse(controller.state.value.visible)
        }

    @Test
    fun clearAllResetsEveryFieldAndRejectsLateImportFromOldDraft() =
        runTest {
            val controller = ReplyComposerController(this, address, FakePostingRepository(), {})
            controller.open(false)
            advanceUntilIdle()
            val old =
                ComposerDraftState(
                    subject = "subject",
                    comment = "comment",
                    email = "email",
                    attachments = files,
                    options = PostingOptions("Neko", sage = true, threadAuthor = true),
                )
            controller.updateDraft(old)
            controller.clearAll()
            advanceUntilIdle()
            val cleared = controller.state.value
            assertEquals("", cleared.draft.subject)
            assertEquals("", cleared.comment)
            assertEquals("", cleared.draft.email)
            assertEquals(PostingOptions(), cleared.draft.options)
            assertEquals(emptyList<ComposerAttachment>(), cleared.attachments)
            assertEquals(emptyList<ByteArray>(), cleared.selectedEmojiImages)
            assertNull(cleared.emojiCaptchaToken)
            controller.updateDraft(old.copy(filesLoading = false))
            assertEquals(cleared.draft, controller.state.value.draft)
        }

    @Test
    fun closeCancelsFilePreparationWithoutPosting() =
        runTest {
            val repository = FakePostingRepository()
            var cancelled = false
            val controller =
                ReplyComposerController(this, address, repository, {}, prepareFiles = { _, _ ->
                    try {
                        awaitCancellation()
                    } finally {
                        cancelled = true
                    }
                })
            controller.open(false)
            advanceUntilIdle()
            controller.updateDraft(ComposerDraftState(attachments = files))
            controller.submit()
            runCurrent()
            controller.close()
            advanceUntilIdle()
            assertTrue(cancelled)
            assertNull(repository.lastDraft)
            assertFalse(controller.state.value.visible)
        }

    @Test
    fun eightFilesMayBeSelectedButInvalidRenameCannotBeSubmitted() =
        runTest {
            val controller = ReplyComposerController(this, address, FakePostingRepository(), {})
            controller.open(false)
            advanceUntilIdle()
            controller.updateDraft(ComposerDraftState(attachments = List(8) { files.first() }))
            assertTrue(controller.state.value.canSubmit)
            controller.updateDraft(
                ComposerDraftState(
                    attachments =
                        listOf(
                            files.first().copy(
                                options = AttachmentEditOptions(customName = "../bad"),
                            ),
                        ),
                ),
            )
            assertFalse(controller.state.value.canSubmit)
        }

    @Test
    fun replyAppliesOptionsAndHonorsBoardRestrictions() =
        runTest {
            val allowed = FakePostingRepository()
            val first = ReplyComposerController(this, address, allowed, {})
            first.open(false)
            advanceUntilIdle()
            first.updateDraft(
                ComposerDraftState(
                    comment = "reply",
                    email = "mail",
                    options = PostingOptions("Neko", sage = true, threadAuthor = true),
                ),
            )
            first.submit()
            advanceUntilIdle()
            assertEquals(PostingOptions("Neko", sage = true, threadAuthor = true), allowed.lastDraft?.options)
            val restricted =
                FakePostingRepository(
                    PostingRules(
                        identity = PostingIdentityRules(namesEnabled = false),
                        features = PostingFeatures(sage = false),
                    ),
                )
            val second = ReplyComposerController(this, address, restricted, {})
            second.open(false)
            advanceUntilIdle()
            second.updateDraft(ComposerDraftState(comment = "reply", options = PostingOptions("Neko", sage = true)))
            second.submit()
            advanceUntilIdle()
            assertEquals(PostingOptions(), restricted.lastDraft?.options)
        }
}
