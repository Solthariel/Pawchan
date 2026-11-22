package com.solthariel.pawchan.composer

import com.solthariel.pawchan.domain.drafts.DraftImageEdit
import com.solthariel.pawchan.domain.drafts.DraftImageOptions
import com.solthariel.pawchan.domain.drafts.MediaDraftOptions
import com.solthariel.pawchan.domain.posting.ReplyAttachment
import com.solthariel.pawchan.feature.composer.R
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class EditedAttachmentOptionsTest {
    @Test
    fun hashAndMetadataAreForcedOnlyForAnEditedImage() {
        val original = ComposerAttachment(ReplyAttachment("image.png", "image/png", byteArrayOf(1)))
        val options = MediaDraftOptions(image = DraftImageOptions(edit = DraftImageEdit(quarterTurns = 1)))
        val edited = original.copy(options = options)
        for (label in listOf(R.string.attachment_unique_hash, R.string.attachment_strip_metadata)) {
            assertTrue(attachmentOptionChecked(edited, label))
            assertTrue(attachmentOptionForced(edited, label))
            assertFalse(attachmentOptionChecked(original, label))
            assertFalse(attachmentOptionForced(original, label))
        }
        assertFalse(edited.options.uniqueHash)
        assertFalse(edited.options.removeMetadata)
        assertFalse(attachmentOptionForced(edited, R.string.attachment_remove_name))
    }
}
