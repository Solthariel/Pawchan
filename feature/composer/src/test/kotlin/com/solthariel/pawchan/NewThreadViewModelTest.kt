package com.solthariel.pawchan

import com.solthariel.pawchan.composer.COMPOSER_SUCCESS_DISPLAY_MILLIS
import com.solthariel.pawchan.composer.ComposerAttachment
import com.solthariel.pawchan.composer.ComposerDraftState
import com.solthariel.pawchan.composer.NewThreadViewModel
import com.solthariel.pawchan.domain.model.BoardAddress
import com.solthariel.pawchan.domain.model.PostId
import com.solthariel.pawchan.domain.model.ThreadAddress
import com.solthariel.pawchan.domain.model.ThreadId
import com.solthariel.pawchan.domain.posting.EmojiCaptchaStep
import com.solthariel.pawchan.domain.posting.NewThreadDraft
import com.solthariel.pawchan.domain.posting.PostingCaptchaState
import com.solthariel.pawchan.domain.posting.PostingFeatures
import com.solthariel.pawchan.domain.posting.PostingIdentityRules
import com.solthariel.pawchan.domain.posting.PostingOptions
import com.solthariel.pawchan.domain.posting.PostingOutcome
import com.solthariel.pawchan.domain.posting.PostingRepository
import com.solthariel.pawchan.domain.posting.PostingRules
import com.solthariel.pawchan.domain.posting.ReplyAttachment
import com.solthariel.pawchan.domain.posting.ReplyDraft
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.Assert.assertEquals
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class NewThreadViewModelTest {
    @Test
    fun createsThreadAfterCaptchaSkip() =
        runTest {
            Dispatchers.setMain(StandardTestDispatcher(testScheduler))
            try {
                val repository = FakeNewThreadPosting()
                val viewModel =
                    NewThreadViewModel(defaultBoardAddress(), repository, prepareFiles = { files, _ ->
                        PostingOutcome.Success(files.map { it.source })
                    }, preparationDispatcher = Dispatchers.Unconfined)
                advanceUntilIdle()
                viewModel.updateDraft(
                    ComposerDraftState(
                        subject = "Тема",
                        comment = "Первый пост",
                        attachments =
                            listOf(
                                ComposerAttachment(ReplyAttachment("cat.png", "image/png", byteArrayOf(1, 2, 3))),
                            ),
                    ),
                )

                viewModel.submit()
                advanceUntilIdle()

                assertEquals("Тема", repository.draft?.subject)
                assertEquals(
                    99L,
                    viewModel.state.value.createdThread
                        ?.thread
                        ?.value,
                )
            } finally {
                Dispatchers.resetMain()
            }
        }

    @Test
    fun successIsShownInPlaceBeforeNavigatingAndCannotBeSubmittedTwice() =
        runTest {
            Dispatchers.setMain(StandardTestDispatcher(testScheduler))
            try {
                val viewModel =
                    NewThreadViewModel(defaultBoardAddress(), FakeNewThreadPosting(), prepareFiles = { files, _ ->
                        PostingOutcome.Success(files.map { it.source })
                    }, preparationDispatcher = Dispatchers.Unconfined)
                advanceUntilIdle()
                val draft =
                    ComposerDraftState(
                        comment = "Первый пост",
                        attachments =
                            listOf(ComposerAttachment(ReplyAttachment("cat.png", "image/png", byteArrayOf(1)))),
                    )
                viewModel.updateDraft(draft)
                viewModel.submit()
                runCurrent()
                assertEquals(true, viewModel.state.value.progress.sent)
                assertEquals(draft, viewModel.state.value.draft)
                assertEquals(false, viewModel.state.value.canSubmit)
                assertEquals(null, viewModel.state.value.createdThread)
                viewModel.reloadCaptcha()
                viewModel.clearAll()
                viewModel.submit()
                assertEquals(true, viewModel.state.value.progress.sent)
                advanceTimeBy(COMPOSER_SUCCESS_DISPLAY_MILLIS)
                runCurrent()
                assertEquals(
                    99L,
                    viewModel.state.value.createdThread
                        ?.thread
                        ?.value,
                )
            } finally {
                Dispatchers.resetMain()
            }
        }

    @Test
    fun clearNewThreadAlsoClearsEmailSubjectFilesAndLateImport() =
        runTest {
            Dispatchers.setMain(StandardTestDispatcher(testScheduler))
            try {
                val viewModel =
                    NewThreadViewModel(
                        defaultBoardAddress(),
                        FakeNewThreadPosting(),
                        preparationDispatcher = Dispatchers.Unconfined,
                    )
                advanceUntilIdle()
                val previous =
                    ComposerDraftState(
                        subject = "subject",
                        comment = "comment",
                        email = "email",
                        options = PostingOptions("Neko", sage = true, threadAuthor = true),
                        attachments =
                            listOf(
                                ComposerAttachment(ReplyAttachment("cat.png", "image/png", byteArrayOf(1))),
                            ),
                    )
                viewModel.updateDraft(previous)
                viewModel.clearAll()
                advanceUntilIdle()
                val draft = viewModel.state.value.draft
                assertEquals("", draft.subject)
                assertEquals("", draft.comment)
                assertEquals("", draft.email)
                assertEquals(PostingOptions(), draft.options)
                assertEquals(emptyList<ComposerAttachment>(), draft.attachments)
                assertEquals(false, viewModel.state.value.canSubmit)
                viewModel.updateDraft(previous)
                assertEquals(draft, viewModel.state.value.draft)
            } finally {
                Dispatchers.resetMain()
            }
        }

    @Test
    fun hiddenSubjectAndNameAreNotSentButAuthorOptionIsPreserved() =
        runTest {
            Dispatchers.setMain(StandardTestDispatcher(testScheduler))
            try {
                val rules =
                    PostingRules(
                        identity = PostingIdentityRules(namesEnabled = false),
                        features = PostingFeatures(subject = false, sage = false),
                    )
                val repository = FakeNewThreadPosting(rules)
                val viewModel =
                    NewThreadViewModel(defaultBoardAddress(), repository, prepareFiles = { files, _ ->
                        PostingOutcome.Success(files.map { it.source })
                    }, preparationDispatcher = Dispatchers.Unconfined)
                advanceUntilIdle()
                val file = ComposerAttachment(ReplyAttachment("cat.png", "image/png", byteArrayOf(1)))
                viewModel.updateDraft(
                    ComposerDraftState(
                        subject = "hidden",
                        comment = "comment",
                        email = "mail",
                        attachments = listOf(file),
                        options = PostingOptions("hidden name", sage = true, threadAuthor = true),
                    ),
                )
                viewModel.submit()
                advanceUntilIdle()
                assertEquals("", repository.draft?.subject)
                assertEquals("mail", repository.draft?.email)
                assertEquals(PostingOptions(threadAuthor = true), repository.draft?.options)
            } finally {
                Dispatchers.resetMain()
            }
        }
}

private class FakeNewThreadPosting(
    private val rules: PostingRules = PostingRules(),
) : PostingRepository {
    override suspend fun loadRules(board: BoardAddress): PostingOutcome<PostingRules> = PostingOutcome.Success(rules)

    var draft: NewThreadDraft? = null
        private set

    override suspend fun loadCaptcha(
        board: BoardAddress,
        thread: ThreadId?,
    ): PostingOutcome<PostingCaptchaState> = PostingOutcome.Success(PostingCaptchaState(null))

    override suspend fun selectEmoji(
        challengeId: String,
        emojiNumber: Int,
    ): PostingOutcome<EmojiCaptchaStep> = PostingOutcome.Success(EmojiCaptchaStep.Solved("token"))

    override suspend fun submitReply(
        address: ThreadAddress,
        draft: ReplyDraft,
    ): PostingOutcome<PostId> = error("Not used")

    override suspend fun createThread(
        board: BoardAddress,
        draft: NewThreadDraft,
    ): PostingOutcome<ThreadAddress> {
        this.draft = draft
        return PostingOutcome.Success(
            ThreadAddress(board, requireNotNull(ThreadId.parse(99L))),
        )
    }
}
