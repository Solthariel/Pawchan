package com.solthariel.pawchan.media

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class LocalVideoDimensionsTest {
    @Test fun readsValidAttachmentDimensions() {
        val dimensions = requireNotNull(localVideoDimensions("938×642"))
        assertEquals(938, dimensions.width)
        assertEquals(642, dimensions.height)
    }

    @Test fun ignoresMissingAndInvalidDimensions() {
        listOf(null, "938x642", "0×642", "938×-1", "a×3", "1×2×3").forEach {
            assertNull(localVideoDimensions(it))
        }
    }
}
