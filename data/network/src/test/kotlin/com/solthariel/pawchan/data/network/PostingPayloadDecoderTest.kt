package com.solthariel.pawchan.data.network

import com.solthariel.pawchan.domain.posting.EmojiCaptchaStep
import com.solthariel.pawchan.domain.posting.PostingFailure
import com.solthariel.pawchan.domain.posting.PostingOutcome
import kotlinx.serialization.json.Json
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Base64

class PostingPayloadDecoderTest {
    private val decoder = PostingPayloadDecoder(Json)

    @Test
    fun decodesRequiredEmojiCaptchaId() {
        val outcome = decoder.decodeCaptchaId("""{"result":1,"id":"emoji-42"}""")

        assertEquals("emoji-42", (outcome as PostingOutcome.Success).value.requiredId())
    }

    @Test
    fun decodesEmojiChallengeContent() {
        val task = Base64.getEncoder().encodeToString(byteArrayOf(1, 2, 3))
        val option = Base64.getEncoder().encodeToString(byteArrayOf(4, 5, 6))
        val outcome =
            decoder.decodeEmojiStep(
                """{"image":"$task","keyboard":["$option"]}""",
            )

        val challenge = (outcome as PostingOutcome.Success).value as EmojiCaptchaStep.Challenge
        assertArrayEquals(byteArrayOf(1, 2, 3), challenge.taskImage)
        assertArrayEquals(byteArrayOf(4, 5, 6), challenge.options.single().image)
        assertEquals(0, challenge.options.single().number)
    }

    @Test
    fun decodesSolvedEmojiToken() {
        val outcome = decoder.decodeEmojiStep("""{"success":"posting-token"}""")

        assertEquals("posting-token", ((outcome as PostingOutcome.Success).value as EmojiCaptchaStep.Solved).token)
    }

    @Test
    fun decodesCreatedPostNumber() {
        val outcome = decoder.decodeReply("""{"num":12345}""")

        assertEquals(12345L, (outcome as PostingOutcome.Success).value.value)
    }

    @Test
    fun decodesCreatedThreadNumber() {
        val board =
            com.solthariel.pawchan.domain.model.BoardAddress(
                site =
                    requireNotNull(
                        com.solthariel.pawchan.domain.model.SiteId
                            .parse("dvach"),
                    ),
                board =
                    requireNotNull(
                        com.solthariel.pawchan.domain.model.BoardId
                            .parse("b"),
                    ),
            )
        val outcome = decoder.decodeNewThread(board, """{"thread":54321}""")

        assertEquals(54321L, (outcome as PostingOutcome.Success).value.thread.value)
    }

    @Test
    fun classifiesCaptchaRejectionByErrorCode() {
        val outcome =
            decoder.decodeReply(
                """{"error":{"code":22,"message":"Captcha invalid"}}""",
            )

        assertTrue((outcome as PostingOutcome.Failure).reason is PostingFailure.CaptchaRejected)
    }
}

private fun CaptchaIdResult.requiredId(): String = (this as CaptchaIdResult.Required).id
