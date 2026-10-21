package com.solthariel.pawchan.data.network

import com.solthariel.pawchan.domain.model.BoardAddress
import com.solthariel.pawchan.domain.model.PostId
import com.solthariel.pawchan.domain.model.ThreadAddress
import com.solthariel.pawchan.domain.model.ThreadId
import com.solthariel.pawchan.domain.posting.EmojiCaptchaChallenge
import com.solthariel.pawchan.domain.posting.EmojiCaptchaStep
import com.solthariel.pawchan.domain.posting.NewThreadDraft
import com.solthariel.pawchan.domain.posting.PostingCaptchaState
import com.solthariel.pawchan.domain.posting.PostingFailure
import com.solthariel.pawchan.domain.posting.PostingOutcome
import com.solthariel.pawchan.domain.posting.PostingProgressObserver
import com.solthariel.pawchan.domain.posting.PostingRepository
import com.solthariel.pawchan.domain.posting.PostingRules
import com.solthariel.pawchan.domain.posting.ReplyDraft
import kotlinx.serialization.json.Json
import okhttp3.Call
import okhttp3.Request

class DvachPostingApi private constructor(
    private val callFactory: Call.Factory,
    private val decoder: PostingPayloadDecoder,
) : PostingRepository {
    override suspend fun loadRules(board: BoardAddress): PostingOutcome<PostingRules> =
        if (!board.isDvachSource()) {
            unsupportedDvachPosting()
        } else {
            execute(PostingRequestFactory.boardRules(board)) { decodePostingRules(board, it) }
        }

    override suspend fun loadCaptcha(
        board: BoardAddress,
        thread: ThreadId?,
    ): PostingOutcome<PostingCaptchaState> =
        if (!board.isDvachSource()) {
            unsupportedDvachPosting()
        } else {
            when (val idOutcome = execute(PostingRequestFactory.captchaId(board, thread), decoder::decodeCaptchaId)) {
                is PostingOutcome.Failure -> idOutcome
                is PostingOutcome.Success -> loadCaptchaContent(idOutcome.value)
            }
        }

    override suspend fun selectEmoji(
        challengeId: String,
        emojiNumber: Int,
    ): PostingOutcome<EmojiCaptchaStep> =
        execute(
            PostingRequestFactory.captchaClick(challengeId, emojiNumber),
            decoder::decodeEmojiStep,
        )

    override suspend fun submitReply(
        address: ThreadAddress,
        draft: ReplyDraft,
        observer: PostingProgressObserver,
    ): PostingOutcome<PostId> =
        if (!address.board.isDvachSource()) {
            unsupportedDvachPosting()
        } else {
            execute(PostingRequestFactory.reply(address, draft, observer), decoder::decodeReply)
        }

    override suspend fun createThread(
        board: BoardAddress,
        draft: NewThreadDraft,
        observer: PostingProgressObserver,
    ): PostingOutcome<ThreadAddress> =
        if (!board.isDvachSource()) {
            unsupportedDvachPosting()
        } else {
            execute(PostingRequestFactory.newThread(board, draft, observer)) { payload ->
                decoder.decodeNewThread(board, payload)
            }
        }

    private suspend fun loadCaptchaContent(result: CaptchaIdResult): PostingOutcome<PostingCaptchaState> =
        when (result) {
            is CaptchaIdResult.Skip -> {
                PostingOutcome.Success(PostingCaptchaState(null, result.uploadLimits))
            }

            is CaptchaIdResult.Required -> {
                when (val step = execute(PostingRequestFactory.captchaShow(result.id), decoder::decodeEmojiStep)) {
                    is PostingOutcome.Failure -> step
                    is PostingOutcome.Success -> step.value.toInitialChallenge(result.id)
                }
            }
        }

    private suspend fun <T> execute(
        request: Request,
        decode: (String) -> PostingOutcome<T>,
    ): PostingOutcome<T> = executePostingRequest(callFactory, request, decode)

    companion object {
        fun create(callFactory: Call.Factory): DvachPostingApi =
            DvachPostingApi(
                callFactory = callFactory,
                decoder = PostingPayloadDecoder(Json { ignoreUnknownKeys = true }),
            )
    }
}

private fun EmojiCaptchaStep.toInitialChallenge(id: String): PostingOutcome<PostingCaptchaState> =
    when (this) {
        is EmojiCaptchaStep.Challenge -> {
            val challenge = EmojiCaptchaChallenge(id, taskImage, options, DVACH_CAPTCHA_LIFETIME_SECONDS)
            PostingOutcome.Success(PostingCaptchaState(challenge))
        }

        is EmojiCaptchaStep.Solved -> {
            PostingOutcome.Failure(PostingFailure.InvalidResponse)
        }
    }

// Dvach website EmojiCaptchaTest.js: CFG.CAPTCHA_TTL || 300. Advisory challenge
// lifetime, not a server-guaranteed lifetime for the solved posting token.
private const val DVACH_CAPTCHA_LIFETIME_SECONDS = 300
