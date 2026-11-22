package com.solthariel.pawchan

import com.solthariel.pawchan.composer.incomingComposerFileName
import org.junit.Assert.assertEquals
import org.junit.Test

class IncomingComposerFileNameTest {
    private val png = byteArrayOf(0x89.toByte(), 0x50, 0x4e, 0x47, 0x0d, 0x0a, 0x1a, 0x0a)

    @Test fun extensionlessKeyboardImagesUseTheirActualFileFormat() {
        assertEquals("attachment.png", incomingComposerFileName("attachment", png))
    }

    @Test fun explicitNamesAndUnsupportedContentAreNotRelabeled() {
        assertEquals("photo.png", incomingComposerFileName("photo.png", png))
        assertEquals("attachment", incomingComposerFileName("attachment", byteArrayOf(1, 2, 3)))
    }
}
