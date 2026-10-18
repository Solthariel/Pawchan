package com.solthariel.pawchan.domain.posting

import com.solthariel.pawchan.domain.model.BoardAddress
import com.solthariel.pawchan.domain.model.PostId
import com.solthariel.pawchan.domain.model.ThreadAddress
import com.solthariel.pawchan.domain.model.ThreadId

data class EmojiCaptchaChallenge(
    val id: String,
    val taskImage: ByteArray,
    val options: List<EmojiCaptchaOption>,
    val lifetimeSeconds: Int? = null,
) {
    init {
        require(id.isNotBlank())
        require(taskImage.isNotEmpty())
        require(options.isNotEmpty())
        require(lifetimeSeconds == null || lifetimeSeconds > 0)
    }
}

data class EmojiCaptchaOption(
    val number: Int,
    val image: ByteArray,
) {
    init {
        require(number >= 0)
        require(image.isNotEmpty())
    }
}

sealed interface EmojiCaptchaStep {
    data class Challenge(
        val taskImage: ByteArray,
        val options: List<EmojiCaptchaOption>,
    ) : EmojiCaptchaStep

    data class Solved(
        val token: String,
    ) : EmojiCaptchaStep
}

data class ReplyDraft(
    val comment: String,
    val emojiCaptchaToken: String?,
    val attachments: List<ReplyAttachment>,
    val email: String = "",
    val options: PostingOptions = PostingOptions(),
) {
    init {
        require(comment.isNotBlank() || attachments.isNotEmpty())
        require(comment.length <= MAX_COMMENT_LENGTH)
        require(emojiCaptchaToken == null || emojiCaptchaToken.isNotBlank())
    }

    companion object {
        const val MAX_COMMENT_LENGTH: Int = 15_000
    }
}

data class NewThreadDraft(
    val subject: String,
    val comment: String,
    val emojiCaptchaToken: String?,
    val attachments: List<ReplyAttachment>,
    val email: String = "",
    val options: PostingOptions = PostingOptions(),
) {
    init {
        require(attachments.isNotEmpty())
        require(subject.length <= MAX_SUBJECT_LENGTH)
        require(options.tag.length <= MAX_TAG_LENGTH)
        require(comment.isNotBlank())
        require(comment.length <= ReplyDraft.MAX_COMMENT_LENGTH)
        require(emojiCaptchaToken == null || emojiCaptchaToken.isNotBlank())
    }

    companion object {
        const val MAX_SUBJECT_LENGTH: Int = 100
        const val MAX_TAG_LENGTH: Int = 8
    }
}

data class ReplyAttachment(
    val fileName: String,
    val mimeType: String?,
    val content: ByteArray,
    val spoiler: Boolean = false,
) {
    init {
        require(fileName.isNotBlank())
        require(content.isNotEmpty())
        require(content.size <= MAX_ATTACHMENT_BYTES)
    }

    companion object {
        const val MAX_ATTACHMENT_BYTES: Int = 50 * 1024 * 1024
    }
}

sealed interface PostingFailure {
    data object NetworkUnavailable : PostingFailure

    data object NetworkTimeout : PostingFailure

    data object SecureConnectionFailed : PostingFailure

    data object InvalidResponse : PostingFailure

    data object CaptchaRejected : PostingFailure

    data object ThreadClosed : PostingFailure

    data class HttpStatus(
        val status: Int,
    ) : PostingFailure

    data class Rejected(
        val reason: String?,
    ) : PostingFailure
}

sealed interface PostingOutcome<out T> {
    data class Success<T>(
        val value: T,
    ) : PostingOutcome<T>

    data class Failure(
        val reason: PostingFailure,
    ) : PostingOutcome<Nothing>
}

interface PostingRepository {
    suspend fun loadRules(board: BoardAddress): PostingOutcome<PostingRules>

    suspend fun loadCaptcha(
        board: BoardAddress,
        thread: ThreadId?,
    ): PostingOutcome<PostingCaptchaState>

    suspend fun selectEmoji(
        challengeId: String,
        emojiNumber: Int,
    ): PostingOutcome<EmojiCaptchaStep>

    suspend fun submitReply(
        address: ThreadAddress,
        draft: ReplyDraft,
    ): PostingOutcome<PostId>

    suspend fun createThread(
        board: BoardAddress,
        draft: NewThreadDraft,
    ): PostingOutcome<ThreadAddress>
}
