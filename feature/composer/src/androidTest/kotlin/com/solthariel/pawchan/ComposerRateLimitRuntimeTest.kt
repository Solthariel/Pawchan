package com.solthariel.pawchan

import android.os.SystemClock
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import coil3.ImageLoader
import com.solthariel.pawchan.composer.ComposerAttachment
import com.solthariel.pawchan.composer.ComposerCaptchaState
import com.solthariel.pawchan.composer.ComposerDraftState
import com.solthariel.pawchan.composer.ComposerFailureState
import com.solthariel.pawchan.composer.ComposerProgressState
import com.solthariel.pawchan.composer.ComposerScreenStatus
import com.solthariel.pawchan.composer.ComposerStatusLine
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
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class ComposerRateLimitRuntimeTest {
    @get:Rule val compose = createComposeRule()
    private val board = BoardAddress(requireNotNull(SiteId.parse("dvach")), requireNotNull(BoardId.parse("b")))

    @Test
    fun countdownDisablesReloadAndDeadlineExpiryEnablesManualRecovery() {
        val context =
            androidx.test.platform.app.InstrumentationRegistry
                .getInstrumentation()
                .targetContext
        val loader = ImageLoader.Builder(context).build()
        val failure =
            mutableStateOf(
                ComposerFailureState(
                    ReplyError.SERVER,
                    retryDeadlineElapsedMillis = SystemClock.elapsedRealtime() + 60_000L,
                ),
            )
        try {
            compose.setContent {
                MaterialTheme {
                    ComposerStatusLine(
                        ComposerCaptchaState(),
                        ComposerScreenStatus(ComposerProgressState(), failure.value, false),
                        {},
                        loader,
                    )
                }
            }
            compose.onNodeWithText("Сервер ограничил запросы. Повторить через", substring = true).assertIsDisplayed()
            compose.onNodeWithContentDescription("Повторить загрузку капчи").assertIsNotEnabled()
            compose.runOnIdle { failure.value = failure.value.copy(retryDeadlineElapsedMillis = 0L) }
            compose.onNodeWithText("Ожидание завершено. Можно повторить загрузку капчи").assertIsDisplayed()
            compose.onNodeWithContentDescription("Повторить загрузку капчи").assertIsEnabled()
        } finally {
            loader.shutdown()
        }
    }

    @Test
    fun replyEditingClearingAndReopeningCannotBypassTheCooldown() {
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
        val repository = RateLimitedPostingFixture()
        lateinit var controller: ReplyComposerController
        try {
            compose.runOnIdle {
                controller =
                    ReplyComposerController(
                        scope,
                        ThreadAddress(board, requireNotNull(ThreadId.parse(42))),
                        repository,
                        {},
                        prepareFiles = { files, _ -> PostingOutcome.Success(files.map { it.source }) },
                        preparationDispatcher = Dispatchers.Unconfined,
                    )
                controller.open(false)
            }
            compose.waitUntil { controller.state.value.captchaSatisfied }
            compose.runOnIdle {
                controller.updateDraft(
                    controller.state.value.draft
                        .copy(comment = "keep this draft"),
                )
                assertTrue(controller.state.value.canSubmit)
                controller.submit()
            }
            compose.waitUntil { controller.state.value.failure.retryDeadlineElapsedMillis != null }
            compose.runOnIdle {
                val deadline = controller.state.value.failure.retryDeadlineElapsedMillis
                val loads = repository.loads
                controller.updateDraft(
                    controller.state.value.draft
                        .copy(comment = "edited draft"),
                )
                controller.submit()
                controller.reloadCaptcha()
                assertEquals("edited draft", controller.state.value.comment)
                assertEquals(deadline, controller.state.value.failure.retryDeadlineElapsedMillis)
                controller.clearAll()
                assertEquals(deadline, controller.state.value.failure.retryDeadlineElapsedMillis)
                controller.close()
                controller.open(false)
                assertEquals(deadline, controller.state.value.failure.retryDeadlineElapsedMillis)
                assertEquals(loads, repository.loads)
                assertEquals(1, repository.posts)
            }
        } finally {
            scope.cancel()
        }
    }

    @Test
    fun newThreadEditingAndReloadDoNotRepeatThePostDuringCooldown() {
        val repository = RateLimitedPostingFixture()
        lateinit var model: NewThreadViewModel
        compose.runOnIdle {
            model =
                NewThreadViewModel(
                    board,
                    repository,
                    prepareFiles = { files, _ -> PostingOutcome.Success(files.map { it.source }) },
                    preparationDispatcher = Dispatchers.Unconfined,
                )
        }
        compose.waitUntil { model.state.value.captchaSatisfied }
        compose.runOnIdle {
            model.updateDraft(
                ComposerDraftState(
                    comment = "thread draft",
                    attachments =
                        listOf(ComposerAttachment(ReplyAttachment("cat.png", "image/png", byteArrayOf(1)))),
                ),
            )
            assertTrue(model.state.value.canSubmit)
            model.submit()
        }
        compose.waitUntil { model.state.value.failure.retryDeadlineElapsedMillis != null }
        compose.runOnIdle {
            val deadline = model.state.value.failure.retryDeadlineElapsedMillis
            val loads = repository.loads
            model.updateDraft(
                model.state.value.draft
                    .copy(comment = "keep edited thread"),
            )
            model.submit()
            model.reloadCaptcha()
            assertEquals("keep edited thread", model.state.value.comment)
            assertEquals(deadline, model.state.value.failure.retryDeadlineElapsedMillis)
            assertNotNull(deadline)
            assertEquals(loads, repository.loads)
            assertEquals(1, repository.posts)
            model.close {}
        }
    }
}

private class RateLimitedPostingFixture : PostingRepository {
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
    ): PostingOutcome<PostId> = limited()

    override suspend fun createThread(
        board: BoardAddress,
        draft: NewThreadDraft,
        observer: PostingProgressObserver,
    ): PostingOutcome<ThreadAddress> = limited()

    private fun limited(): PostingOutcome.Failure {
        posts++
        return PostingOutcome.Failure(PostingFailure.HttpStatus(429, 120_000L))
    }
}
