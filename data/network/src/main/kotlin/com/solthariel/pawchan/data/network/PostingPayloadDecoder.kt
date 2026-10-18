package com.solthariel.pawchan.data.network

import com.solthariel.pawchan.domain.model.BoardAddress
import com.solthariel.pawchan.domain.model.PostId
import com.solthariel.pawchan.domain.model.ThreadAddress
import com.solthariel.pawchan.domain.model.ThreadId
import com.solthariel.pawchan.domain.posting.AccountUploadLimits
import com.solthariel.pawchan.domain.posting.EmojiCaptchaOption
import com.solthariel.pawchan.domain.posting.EmojiCaptchaStep
import com.solthariel.pawchan.domain.posting.PostingFailure
import com.solthariel.pawchan.domain.posting.PostingOutcome
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.longOrNull
import java.util.Base64

internal class PostingPayloadDecoder(
    private val json: Json,
) {
    fun decodeCaptchaId(payload: String): PostingOutcome<CaptchaIdResult> {
        val root = json.parseToJsonElement(payload) as? JsonObject ?: return invalid()
        return when (root.int("result")) {
            CAPTCHA_REQUIRED -> {
                root
                    .text("id")
                    ?.takeIf(String::isNotBlank)
                    ?.let { PostingOutcome.Success(CaptchaIdResult.Required(it)) }
                    ?: invalid()
            }

            CAPTCHA_PASS -> {
                PostingOutcome.Success(CaptchaIdResult.Skip(decodeAccountUploadLimits(root)))
            }

            CAPTCHA_NOT_REQUIRED -> {
                PostingOutcome.Success(CaptchaIdResult.Skip())
            }

            else -> {
                PostingOutcome.Failure(PostingFailure.Rejected(root.reason()))
            }
        }
    }

    fun decodeEmojiStep(payload: String): PostingOutcome<EmojiCaptchaStep> {
        val root = json.parseToJsonElement(payload) as? JsonObject ?: return invalid()
        val success = root.text("success")
        return if (!success.isNullOrBlank()) {
            PostingOutcome.Success(EmojiCaptchaStep.Solved(success))
        } else {
            root.challengeStep()?.let(PostingOutcome<EmojiCaptchaStep>::Success) ?: invalid()
        }
    }

    fun decodeReply(payload: String): PostingOutcome<PostId> {
        val root = json.parseToJsonElement(payload) as? JsonObject ?: return invalid()
        val postId = root.long("num")?.let(PostId::parse)
        return if (postId != null) {
            PostingOutcome.Success(postId)
        } else {
            PostingOutcome.Failure(root.failure())
        }
    }

    fun decodeNewThread(
        board: BoardAddress,
        payload: String,
    ): PostingOutcome<ThreadAddress> {
        val root = json.parseToJsonElement(payload) as? JsonObject ?: return invalid()
        val address =
            root
                .long("thread")
                ?.let(ThreadId::parse)
                ?.let { thread -> ThreadAddress(board, thread) }
        return if (address != null) {
            PostingOutcome.Success(address)
        } else {
            PostingOutcome.Failure(root.failure())
        }
    }

    private fun JsonObject.challengeStep(): EmojiCaptchaStep.Challenge? {
        val taskImage = text("image")?.decodeImage()
        val keyboard = get("keyboard") as? JsonArray
        val options =
            keyboard
                ?.mapIndexedNotNull { index, element ->
                    element.jsonPrimitive.contentOrNull
                        ?.decodeImage()
                        ?.let { EmojiCaptchaOption(index, it) }
                }.orEmpty()
        return if (taskImage != null && options.isNotEmpty()) {
            EmojiCaptchaStep.Challenge(taskImage, options)
        } else {
            null
        }
    }

    private fun JsonObject.failure(): PostingFailure {
        val errorObject = get("error") as? JsonObject
        val code = errorObject?.int("code")
        val reason = errorObject?.text("message") ?: reason()
        return when (code) {
            ERROR_THREAD_CLOSED -> PostingFailure.ThreadClosed
            ERROR_CAPTCHA, ERROR_CAPTCHA_EXPIRED, ERROR_CAPTCHA_INVALID -> PostingFailure.CaptchaRejected
            else -> PostingFailure.Rejected(reason)
        }
    }

    private fun JsonObject.reason(): String? =
        sequenceOf("reason", "description", "error")
            .mapNotNull { name -> text(name) }
            .firstOrNull()
            ?.take(MAX_REASON_LENGTH)

    private fun JsonObject.text(name: String): String? =
        (get(name) as? JsonPrimitive)
            ?.contentOrNull
            ?.trim()
            ?.takeIf(String::isNotEmpty)

    private fun JsonObject.int(name: String): Int? = text(name)?.toIntOrNull()

    private fun JsonObject.long(name: String): Long? {
        val directValue = (get(name) as? JsonPrimitive)?.longOrNull
        return directValue ?: text(name)?.toLongOrNull()
    }
}

internal sealed interface CaptchaIdResult {
    data class Required(
        val id: String,
    ) : CaptchaIdResult

    data class Skip(
        val uploadLimits: AccountUploadLimits? = null,
    ) : CaptchaIdResult
}

private fun String.decodeImage(): ByteArray? =
    runCatching {
        Base64.getDecoder().decode(substringAfter("base64,", this))
    }.getOrNull()?.takeIf(ByteArray::isNotEmpty)

private fun <T> invalid(): PostingOutcome<T> = PostingOutcome.Failure(PostingFailure.InvalidResponse)

private const val CAPTCHA_REQUIRED = 1
private const val CAPTCHA_PASS = 2
private const val CAPTCHA_NOT_REQUIRED = 3
private const val ERROR_THREAD_CLOSED = 7
private const val ERROR_CAPTCHA = 5
private const val ERROR_CAPTCHA_EXPIRED = 21
private const val ERROR_CAPTCHA_INVALID = 22
private const val MAX_REASON_LENGTH = 300
