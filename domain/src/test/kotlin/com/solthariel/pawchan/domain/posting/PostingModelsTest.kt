package com.solthariel.pawchan.domain.posting

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class PostingModelsTest {
    @Test
    fun allowsAttachmentOnlyReply() {
        val attachment =
            ReplyAttachment(
                fileName = "cat.png",
                mimeType = "image/png",
                content = byteArrayOf(1, 2, 3),
            )

        val draft = ReplyDraft(comment = "", emojiCaptchaToken = null, attachments = listOf(attachment))

        assertEquals("cat.png", draft.attachments.first().fileName)
    }

    @Test
    fun rejectsEmptyReplyWithoutAttachment() {
        assertThrows(IllegalArgumentException::class.java) {
            ReplyDraft(comment = "", emojiCaptchaToken = null, attachments = emptyList())
        }
    }
}
