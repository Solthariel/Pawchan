package com.solthariel.pawchan

import com.solthariel.pawchan.composer.AttachmentEditOptions
import com.solthariel.pawchan.composer.ComposerAttachment
import com.solthariel.pawchan.composer.ComposerDraftCoordinator
import com.solthariel.pawchan.composer.ComposerDraftState
import com.solthariel.pawchan.composer.ReplyComposerController
import com.solthariel.pawchan.composer.toStoredDraft
import com.solthariel.pawchan.domain.drafts.ComposerDraftKey
import com.solthariel.pawchan.domain.drafts.ComposerDraftPayload
import com.solthariel.pawchan.domain.drafts.ComposerDraftRepository
import com.solthariel.pawchan.domain.model.ThreadAddress
import com.solthariel.pawchan.domain.model.ThreadId
import com.solthariel.pawchan.domain.posting.PostingOptions
import com.solthariel.pawchan.domain.posting.ReplyAttachment
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.IOException

@OptIn(ExperimentalCoroutinesApi::class)
class ComposerDraftPersistenceTest {
    private val board = defaultBoardAddress()
    private val address = ThreadAddress(board, requireNotNull(ThreadId.parse(42)))
    private val key = ComposerDraftKey(board, address.thread)
    private val draft =
        ComposerDraftState(
            comment = "comment",
            subject = "subject",
            email = "mail",
            options = PostingOptions("name", sage = true),
            attachments =
                listOf(
                    ComposerAttachment(
                        ReplyAttachment("file.png", "image/png", byteArrayOf(1, 2)),
                        options = AttachmentEditOptions(customName = "renamed", spoiler = true),
                    ),
                ),
        )

    @Test fun closingReleasesMediaAndReopeningRestoresEveryDraftOption() =
        runTest {
            val disk = TestComposerDraftRepository()
            val storage =
                ComposerDraftCoordinator(
                    this,
                    disk,
                    { testScheduler.currentTime },
                    StandardTestDispatcher(testScheduler),
                )
            val controller = ReplyComposerController(this, address, FakePostingRepository(), {}, drafts = storage)
            controller.open(false)
            advanceUntilIdle()
            controller.updateDraft(draft)
            controller.close()
            advanceUntilIdle()
            assertFalse(controller.state.value.visible)
            assertTrue(
                controller.state.value.attachments
                    .isEmpty(),
            )
            assertEquals(0, storage.pendingWrites)
            controller.open(false)
            advanceUntilIdle()
            val restored = controller.state.value.draft
            assertEquals(draft.comment, restored.comment)
            assertEquals(draft.options, restored.options)
            assertEquals(draft.attachments.single().options, restored.attachments.single().options)
            assertNull(controller.state.value.emojiCaptchaToken)
        }

    @Test fun dirtyUpdatesAreCoalescedButNotPostponedBeyondTenSeconds() =
        runTest {
            val disk = TestComposerDraftRepository()
            val storage =
                ComposerDraftCoordinator(
                    this,
                    disk,
                    { testScheduler.currentTime },
                    StandardTestDispatcher(testScheduler),
                )
            repeat(20) { number ->
                storage.schedule(key, ComposerDraftState(comment = "$number"))
                runCurrent()
                advanceTimeBy(500)
            }
            runCurrent()
            assertEquals(1, disk.writes)
            assertEquals("19", disk.saved[key]?.text?.comment)
            assertEquals(0, storage.pendingWrites)
        }

    @Test fun clearAllCancelsAnOlderPendingSaveAndCannotRestoreDeletedFiles() =
        runTest {
            val disk = TestComposerDraftRepository()
            val storage =
                ComposerDraftCoordinator(
                    this,
                    disk,
                    { testScheduler.currentTime },
                    StandardTestDispatcher(testScheduler),
                )
            val controller = ReplyComposerController(this, address, FakePostingRepository(), {}, drafts = storage)
            controller.open(false)
            advanceUntilIdle()
            controller.updateDraft(draft)
            controller.clearAll()
            advanceUntilIdle()
            controller.close()
            advanceUntilIdle()
            controller.open(false)
            advanceUntilIdle()
            assertEquals("", controller.state.value.comment)
            assertTrue(
                controller.state.value.attachments
                    .isEmpty(),
            )
            assertNull(disk.saved[key])
        }

    @Test fun failedSaveKeepsTheFormAndItsMediaOpenInsteadOfDroppingThem() =
        runTest {
            val disk = TestComposerDraftRepository()
            val storage =
                ComposerDraftCoordinator(
                    this,
                    disk,
                    { testScheduler.currentTime },
                    StandardTestDispatcher(testScheduler),
                )
            val controller = ReplyComposerController(this, address, FakePostingRepository(), {}, drafts = storage)
            controller.open(false)
            advanceUntilIdle()
            controller.updateDraft(draft)
            disk.failWrites = true
            controller.close()
            advanceUntilIdle()
            assertTrue(controller.state.value.visible)
            assertEquals(draft.attachments, controller.state.value.attachments)
            assertTrue(
                controller.state.value.rejectionReason
                    .orEmpty()
                    .contains("сохранить"),
            )
            assertTrue(storage.notices.value?.cause is IOException)
        }

    @Test fun failedRestoreIsNeverReplacedWithBlankOnCloseOrBackground() =
        runTest {
            val disk = TestComposerDraftRepository()
            disk.saved[key] = draft.toStoredDraft()
            disk.failReads = true
            val storage =
                ComposerDraftCoordinator(
                    this,
                    disk,
                    { testScheduler.currentTime },
                    StandardTestDispatcher(testScheduler),
                )
            val controller = ReplyComposerController(this, address, FakePostingRepository(), {}, drafts = storage)
            controller.open(false)
            advanceUntilIdle()
            controller.saveDraftNow()
            controller.close()
            advanceUntilIdle()
            assertEquals(draft.comment, disk.saved[key]?.text?.comment)
            assertEquals(0, disk.writes)
            disk.failReads = false
            controller.open(false)
            advanceUntilIdle()
            assertEquals(draft.comment, controller.state.value.comment)
        }

    @Test fun successfulPostCancelsAutosaveAndRemovesOnlyItsOwnDraft() =
        runTest {
            val disk = TestComposerDraftRepository()
            val storage =
                ComposerDraftCoordinator(
                    this,
                    disk,
                    { testScheduler.currentTime },
                    StandardTestDispatcher(testScheduler),
                )
            val newKey = ComposerDraftKey(board)
            disk.saved[newKey] = ComposerDraftState(comment = "new thread").toStoredDraft()
            val controller = ReplyComposerController(this, address, FakePostingRepository(), {}, drafts = storage)
            controller.open(false)
            advanceUntilIdle()
            controller.updateDraft(ComposerDraftState(comment = "reply"))
            controller.submit()
            advanceUntilIdle()
            assertNull(disk.saved[key])
            assertEquals("new thread", disk.saved[newKey]?.text?.comment)
        }
}

internal class TestComposerDraftRepository : ComposerDraftRepository {
    val saved = mutableMapOf<ComposerDraftKey, ComposerDraftPayload>()
    var failReads = false
    var failWrites = false
    var writes = 0

    override suspend fun load(key: ComposerDraftKey): ComposerDraftPayload? {
        if (failReads) throw IOException("Unreadable draft")
        return saved[key]
    }

    override suspend fun save(
        key: ComposerDraftKey,
        draft: ComposerDraftPayload,
    ) {
        if (failWrites) throw IOException("disk full")
        writes++
        if (draft.empty) saved.remove(key) else saved[key] = draft
    }

    override suspend fun remove(key: ComposerDraftKey) {
        saved.remove(key)
    }
}
