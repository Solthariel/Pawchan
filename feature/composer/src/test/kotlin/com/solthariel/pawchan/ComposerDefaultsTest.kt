package com.solthariel.pawchan

import com.solthariel.pawchan.composer.AttachmentEditOptions
import com.solthariel.pawchan.composer.ComposerAttachment
import com.solthariel.pawchan.composer.attachmentUploadName
import com.solthariel.pawchan.composer.withDefaults
import com.solthariel.pawchan.domain.posting.PostingFeatures
import com.solthariel.pawchan.domain.posting.PostingRules
import com.solthariel.pawchan.domain.posting.ReplyAttachment
import com.solthariel.pawchan.domain.settings.AttachmentDefaults
import com.solthariel.pawchan.domain.settings.ImageEncodingDefaults
import com.solthariel.pawchan.domain.settings.ReencodeFormat
import com.solthariel.pawchan.domain.settings.UploadNameMode
import com.solthariel.pawchan.domain.settings.UploadNaming
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class ComposerDefaultsTest {
    private val image =
        ComposerAttachment(
            ReplyAttachment("photo.png", "image/png", byteArrayOf(-119, 80, 78, 71, 13, 10, 26, 10)),
        )
    private val rules = PostingRules(features = PostingFeatures(attachmentSpoilers = true))

    @Test
    fun defaultsDoNotMutateSourceAndStayEditablePerFile() {
        val defaults = AttachmentDefaults(uniqueHash = true, removeMetadata = true, spoiler = true)
        val imported = image.withDefaults(defaults, rules)
        assertSame(image.source, imported.source)
        assertEquals(AttachmentEditOptions(), image.options)
        assertTrue(imported.options.uniqueHash)
        assertTrue(imported.options.removeMetadata)
        assertTrue(imported.options.spoiler)
        val edited = imported.copy(options = imported.options.copy(spoiler = false))
        assertFalse(edited.options.spoiler)
        assertTrue(imported.options.spoiler)
    }

    @Test
    fun nameModesPreserveActualOutputExtension() {
        val removed = image.withDefaults(AttachmentDefaults(naming = UploadNaming(UploadNameMode.REMOVE)), rules)
        assertEquals("file.png", attachmentUploadName(removed))
        val custom =
            image.withDefaults(
                AttachmentDefaults(
                    naming = UploadNaming(UploadNameMode.CUSTOM, "кот"),
                    reencoding = ImageEncodingDefaults(ReencodeFormat.JPEG, 80, 2),
                ),
                rules,
            )
        assertEquals("кот.jpg", attachmentUploadName(custom))
        assertEquals(2, custom.options.reencoding?.scaleDivisor)
    }

    @Test
    fun randomNameIsStableForTheFileButDifferentForNewImports() {
        val defaults = AttachmentDefaults(naming = UploadNaming(UploadNameMode.RANDOM))
        val first = image.withDefaults(defaults, rules)
        val second = image.withDefaults(defaults, rules)
        assertNotEquals(first.displayName, second.displayName)
        assertEquals(first.displayName, first.copy().displayName)
        assertTrue(first.displayName.endsWith(".png"))
    }

    @Test
    fun unsupportedImageConversionAndSpoilersAreNotApplied() {
        val defaults = AttachmentDefaults(spoiler = true, reencoding = ImageEncodingDefaults(ReencodeFormat.JPEG))
        val restricted = image.withDefaults(defaults, PostingRules(extensions = setOf("png")))
        assertNull(restricted.options.reencoding)
        assertFalse(restricted.options.spoiler)
        val unknown =
            ComposerAttachment(ReplyAttachment("other.bin", null, byteArrayOf(1)))
                .withDefaults(AttachmentDefaults(uniqueHash = true, removeMetadata = true), rules)
        assertFalse(unknown.options.uniqueHash)
        assertFalse(unknown.options.removeMetadata)
    }

    @Test
    fun invalidCustomNameFallsBackToSafeBase() {
        val imported =
            image.withDefaults(
                AttachmentDefaults(naming = UploadNaming(UploadNameMode.CUSTOM, "../secret")),
                rules,
            )
        assertEquals("image.png", imported.displayName)
        assertTrue(imported.validName)
    }
}
