package com.solthariel.pawchan.composer

import com.solthariel.pawchan.domain.drafts.DraftImageEdit
import com.solthariel.pawchan.domain.drafts.DraftImageOptions
import com.solthariel.pawchan.domain.drafts.MediaDraftOptions
import com.solthariel.pawchan.domain.posting.ReplyAttachment
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Test

class ImageRecipeUploadSafetyTest {
    @Test
    fun unrenderedRecipeCannotFallBackToUneditedSource() {
        val file =
            ComposerAttachment(
                source = ReplyAttachment("image.png", "image/png", byteArrayOf(1, 2, 3)),
                options = MediaDraftOptions(image = DraftImageOptions(edit = DraftImageEdit(quarterTurns = 1))),
            )
        assertNull(file.preparedUpload)
        assertThrows(IllegalArgumentException::class.java) { prepareAttachment(file) }
    }

    @Test
    fun staleUneditedCacheDoesNotBypassRecipeProtection() {
        val source = ReplyAttachment("image.png", "image/png", byteArrayOf(1, 2, 3))
        val old = MediaDraftOptions()
        val edited = old.copy(image = DraftImageOptions(edit = DraftImageEdit(quarterTurns = 1)))
        val file =
            ComposerAttachment(
                source = source,
                options = edited,
                prepared = ComposerPreparedUpload(source, old, source),
            )
        assertNull(file.preparedUpload)
        assertThrows(IllegalArgumentException::class.java) { prepareAttachment(file) }
    }
}
