package com.solthariel.pawchan

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import coil3.ImageLoader
import com.solthariel.pawchan.composer.ComposerAttachment
import com.solthariel.pawchan.composer.ComposerCaptchaState
import com.solthariel.pawchan.composer.ComposerDraftState
import com.solthariel.pawchan.composer.ComposerFailureState
import com.solthariel.pawchan.composer.ComposerProgressState
import com.solthariel.pawchan.composer.ComposerScreenStatus
import com.solthariel.pawchan.composer.ComposerStatusLine
import com.solthariel.pawchan.composer.ComposerUploadBudget
import com.solthariel.pawchan.composer.NewThreadViewModel
import com.solthariel.pawchan.composer.ReplyComposerController
import com.solthariel.pawchan.composer.ReplyError
import com.solthariel.pawchan.domain.model.BoardAddress
import com.solthariel.pawchan.domain.model.BoardId
import com.solthariel.pawchan.domain.model.PostId
import com.solthariel.pawchan.domain.model.SiteId
import com.solthariel.pawchan.domain.model.ThreadAddress
import com.solthariel.pawchan.domain.model.ThreadId
import com.solthariel.pawchan.domain.posting.EmojiCaptchaStep
import com.solthariel.pawchan.domain.posting.NewThreadDraft
import com.solthariel.pawchan.domain.posting.PostingCaptchaState
import com.solthariel.pawchan.domain.posting.PostingFailure
import com.solthariel.pawchan.domain.posting.PostingOutcome
import com.solthariel.pawchan.domain.posting.PostingProgressObserver
import com.solthariel.pawchan.domain.posting.PostingRepository
import com.solthariel.pawchan.domain.posting.PostingRules
import com.solthariel.pawchan.domain.posting.ReplyAttachment
import com.solthariel.pawchan.domain.posting.ReplyDraft
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class ComposerSubmissionRuntimeTest {
    @get:Rule val compose = createComposeRule()
    private val board = BoardAddress(requireNotNull(SiteId.parse("dvach")), requireNotNull(BoardId.parse("b")))

    @Test
    fun reloadRequiresConfirmationAndCancelDoesNothing() {
        val context =
            androidx.test.platform.app.InstrumentationRegistry
                .getInstrumentation()
                .targetContext
        val loader = ImageLoader.Builder(context).build()
        var reloads = 0
        try {
            compose.setContent {
                MaterialTheme {
                    ComposerStatusLine(
                        ComposerCaptchaState(satisfied = true),
                        ComposerScreenStatus(
                            ComposerProgressState(),
                            ComposerFailureState(ReplyError.SUBMISSION_UNCONFIRMED),
                            false,
                            ComposerUploadBudget(null, "Размер ещё рассчитывается"),
                        ),
                        { reloads++ },
                        loader,
                    )
                }
            }
            compose.onNodeWithText("Отправка не подтверждена.", substring = true).assertIsDisplayed()
            compose.onNodeWithContentDescription("Повторить загрузку капчи").performClick()
            compose.onNodeWithText("Проверили результат отправки?").assertIsDisplayed()
            compose.runOnIdle { assertEquals(0, reloads) }
            compose.onNodeWithText("Отмена").performClick()
            compose.runOnIdle { assertEquals(0, reloads) }
            compose.onNodeWithContentDescription("Повторить загрузку капчи").performClick()
            compose.onNodeWithText("Подготовить повторную попытку").performClick()
            compose.runOnIdle { assertEquals(1, reloads) }
        } finally {
            loader.shutdown()
        }
    }

    @Test
    fun replyKeepsDraftAndWarningAndDoesNotAutomaticallySendAgain() {
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
        val posting = UnconfirmedPostingFixture()
        lateinit var controller: ReplyComposerController
        try {
            compose.runOnIdle {
                controller =
                    ReplyComposerController(
                        scope,
                        ThreadAddress(board, requireNotNull(ThreadId.parse(42))),
                        posting,
                        {},
                        preparationDispatcher = Dispatchers.Unconfined,
                    )
                controller.open(false)
            }
            compose.waitUntil { controller.state.value.captchaSatisfied }
            compose.runOnIdle {
                controller.updateDraft(
                    controller.state.value.draft
                        .copy(comment = "keep me"),
                )
                controller.submit()
            }
            compose.waitUntil { controller.state.value.error == ReplyError.SUBMISSION_UNCONFIRMED }
            compose.runOnIdle {
                assertEquals("keep me", controller.state.value.comment)
                controller.updateDraft(
                    controller.state.value.draft
                        .copy(comment = "edited draft"),
                )
                controller.submit()
                assertFalse(controller.state.value.canSubmit)
                assertFalse(controller.state.value.progress.sent)
                assertEquals(1, posting.posts)
                assertEquals("edited draft", controller.state.value.comment)
                controller.reloadCaptcha() // Explicitly confirmed UI action; never a POST.
            }
            compose.waitUntil { controller.state.value.canSubmit }
            compose.runOnIdle { assertEquals(1, posting.posts) }
        } finally {
            scope.cancel()
        }
    }

    @Test
    fun closingAndReopeningTheSameReplyOwnerKeepsTheWarning() {
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
        val posting = UnconfirmedPostingFixture()
        lateinit var controller: ReplyComposerController
        try {
            compose.runOnIdle {
                controller =
                    ReplyComposerController(
                        scope,
                        ThreadAddress(board, requireNotNull(ThreadId.parse(42))),
                        posting,
                        {},
                    )
                controller.open(false)
            }
            compose.waitUntil { controller.state.value.captchaSatisfied }
            compose.runOnIdle {
                controller.updateDraft(
                    controller.state.value.draft
                        .copy(comment = "post"),
                )
                controller.submit()
            }
            compose.waitUntil { controller.state.value.error == ReplyError.SUBMISSION_UNCONFIRMED }
            compose.runOnIdle { controller.close() }
            compose.waitUntil { !controller.state.value.visible }
            compose.runOnIdle {
                val loads = posting.loads
                controller.open(false)
                assertEquals(ReplyError.SUBMISSION_UNCONFIRMED, controller.state.value.error)
                assertFalse(controller.state.value.captchaLoading)
                assertEquals(loads, posting.loads)
                assertEquals(1, posting.posts)
            }
        } finally {
            scope.cancel()
        }
    }

    @Test
    fun newThreadRetainsAttachmentsAndDoesNotPostOnManualCaptchaReload() {
        val posting = UnconfirmedPostingFixture()
        lateinit var model: NewThreadViewModel
        compose.runOnIdle {
            model =
                NewThreadViewModel(
                    board,
                    posting,
                    prepareFiles = { files, _ -> PostingOutcome.Success(files.map { it.source }) },
                    preparationDispatcher = Dispatchers.Unconfined,
                )
        }
        compose.waitUntil { model.state.value.captchaSatisfied }
        compose.runOnIdle {
            model.updateDraft(
                ComposerDraftState(
                    comment = "new thread",
                    attachments = listOf(ComposerAttachment(ReplyAttachment("a.png", "image/png", byteArrayOf(1)))),
                ),
            )
            assertTrue(model.state.value.canSubmit)
            model.submit()
        }
        compose.waitUntil { model.state.value.error == ReplyError.SUBMISSION_UNCONFIRMED }
        compose.runOnIdle {
            assertEquals(1, model.state.value.attachments.size)
            model.updateDraft(
                model.state.value.draft
                    .copy(comment = "edited thread"),
            )
            model.submit()
            assertFalse(model.state.value.canSubmit)
            assertEquals(1, posting.posts)
            assertEquals("edited thread", model.state.value.comment)
            model.reloadCaptcha()
        }
        compose.waitUntil { model.state.value.canSubmit }
        compose.runOnIdle {
            assertEquals(1, posting.posts)
            model.close {}
        }
    }
}

private class UnconfirmedPostingFixture : PostingRepository {
    var loads = 0
    var posts = 0

    override suspend fun loadRules(board: BoardAddress): PostingOutcome<PostingRules> {
        loads++
        return PostingOutcome.Success(PostingRules())
    }

    override suspend fun loadCaptcha(
        board: BoardAddress,
        thread: ThreadId?,
    ): PostingOutcome<PostingCaptchaState> {
        loads++
        return PostingOutcome.Success(PostingCaptchaState(null))
    }

    override suspend fun selectEmoji(
        challengeId: String,
        emojiNumber: Int,
    ): PostingOutcome<EmojiCaptchaStep> = PostingOutcome.Failure(PostingFailure.InvalidResponse)

    override suspend fun submitReply(
        address: ThreadAddress,
        draft: ReplyDraft,
        observer: PostingProgressObserver,
    ): PostingOutcome<PostId> = unconfirmed()

    override suspend fun createThread(
        board: BoardAddress,
        draft: NewThreadDraft,
        observer: PostingProgressObserver,
    ): PostingOutcome<ThreadAddress> = unconfirmed()

    private fun unconfirmed(): PostingOutcome.Failure {
        posts++
        return PostingOutcome.Failure(PostingFailure.SubmissionUnconfirmed)
    }
}
