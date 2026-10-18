package com.solthariel.pawchan

import com.solthariel.pawchan.composer.ComposerAttachment
import com.solthariel.pawchan.composer.ComposerDraftState
import com.solthariel.pawchan.composer.ImageReencoding
import com.solthariel.pawchan.composer.canAttachComposerMedia
import com.solthariel.pawchan.domain.posting.PostingRules
import com.solthariel.pawchan.domain.posting.ReplyAttachment
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ComposerPanelPolicyTest {
    @Test fun attachButtonFollowsTheCurrentAccountLimitAndReturnsAfterRemoval() {
        val four =
            ComposerDraftState(
                attachments =
                    List(4) {
                        ComposerAttachment(ReplyAttachment("file.png", "image/png", byteArrayOf(1)))
                    },
            )
        assertFalse(canAttachComposerMedia(four, PostingRules(maxFiles = 4)))
        assertTrue(canAttachComposerMedia(four, PostingRules(maxFiles = 8)))
        assertTrue(canAttachComposerMedia(four.copy(attachments = four.attachments.dropLast(1)), PostingRules()))
        assertFalse(canAttachComposerMedia(ComposerDraftState(), PostingRules(maxFiles = 0)))
    }

    @Test fun imageReencodingDefaultsToFullQuality() {
        assertEquals(100, ImageReencoding().quality)
    }
}
