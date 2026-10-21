package com.solthariel.pawchan

import com.solthariel.pawchan.domain.model.BoardAddress
import com.solthariel.pawchan.domain.model.PostId
import com.solthariel.pawchan.domain.model.ThreadAddress
import com.solthariel.pawchan.domain.model.ThreadId
import com.solthariel.pawchan.domain.posting.AccountUploadLimits
import com.solthariel.pawchan.domain.posting.EmojiCaptchaChallenge
import com.solthariel.pawchan.domain.posting.EmojiCaptchaStep
import com.solthariel.pawchan.domain.posting.NewThreadDraft
import com.solthariel.pawchan.domain.posting.PostingCaptchaState
import com.solthariel.pawchan.domain.posting.PostingOutcome
import com.solthariel.pawchan.domain.posting.PostingProgressObserver
import com.solthariel.pawchan.domain.posting.PostingRepository
import com.solthariel.pawchan.domain.posting.PostingRules
import com.solthariel.pawchan.domain.posting.ReplyDraft
import kotlinx.coroutines.CompletableDeferred

private typealias CaptchaOutcome = PostingOutcome<PostingCaptchaState>

internal class FakePostingRepository(
    private val rules: PostingRules =
        PostingRules(
            features =
                com.solthariel.pawchan.domain.posting
                    .PostingFeatures(attachmentSpoilers = true),
        ),
) : PostingRepository {
    var accountLimits: AccountUploadLimits? = null
    var challenge: EmojiCaptchaChallenge? = null
    var captchaLoads = 0
    var ruleLoads = 0
    var selectionGate: CompletableDeferred<Unit>? = null
    var nextStep: EmojiCaptchaStep = EmojiCaptchaStep.Solved("token")

    override suspend fun loadRules(board: BoardAddress): PostingOutcome<PostingRules> {
        ruleLoads++
        return PostingOutcome.Success(rules)
    }

    var lastDraft: ReplyDraft? = null
        private set

    override suspend fun loadCaptcha(
        board: BoardAddress,
        thread: ThreadId?,
    ): CaptchaOutcome {
        captchaLoads++
        return PostingOutcome.Success(PostingCaptchaState(challenge, accountLimits))
    }

    override suspend fun selectEmoji(
        challengeId: String,
        emojiNumber: Int,
    ): PostingOutcome<EmojiCaptchaStep> {
        selectionGate?.await()
        return PostingOutcome.Success(nextStep)
    }

    override suspend fun submitReply(
        address: ThreadAddress,
        draft: ReplyDraft,
        observer: PostingProgressObserver,
    ): PostingOutcome<PostId> {
        lastDraft = draft
        return PostingOutcome.Success(requireNotNull(PostId.parse(44L)))
    }

    override suspend fun createThread(
        board: BoardAddress,
        draft: NewThreadDraft,
        observer: PostingProgressObserver,
    ): PostingOutcome<ThreadAddress> =
        PostingOutcome.Success(
            ThreadAddress(board, requireNotNull(ThreadId.parse(45L))),
        )
}
