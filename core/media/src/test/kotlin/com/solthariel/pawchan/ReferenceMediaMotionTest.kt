package com.solthariel.pawchan

import com.solthariel.pawchan.media.ImageTransform
import com.solthariel.pawchan.media.ImageViewport
import com.solthariel.pawchan.media.ReferenceMediaMotion
import org.junit.Assert.assertEquals
import org.junit.Test

class ReferenceMediaMotionTest {
    @Test
    fun upwardCloseFadesTheWholeBackdropAndShrinksThePhoto() {
        assertEquals(1f, ReferenceMediaMotion.imageAlpha(0f, 400f), 0f)
        assertEquals(0.5f, ReferenceMediaMotion.imageAlpha(-100f, 400f), 0f)
        assertEquals(0.9f, ReferenceMediaMotion.imageScale(-100f, 400f), 0.0001f)
        assertEquals(0f, ReferenceMediaMotion.backgroundAlpha(-100f, 400f), 0f)
        assertEquals(0f, ReferenceMediaMotion.imageAlpha(-300f, 400f), 0f)
        assertEquals(0.6f, ReferenceMediaMotion.imageScale(-300f, 400f), 0.0001f)
    }

    @Test
    fun downwardCloseKeepsTheGalleryBackdrop() {
        assertEquals(1f, ReferenceMediaMotion.backgroundAlpha(100f, 400f), 0f)
        assertEquals(0.5f, ReferenceMediaMotion.imageAlpha(100f, 400f), 0f)
    }

    @Test
    fun settlementUsesReferenceDurationsAndCaps() {
        assertEquals(500, ReferenceMediaMotion.closeDuration(200f, 0f))
        assertEquals(150, ReferenceMediaMotion.closeDuration(1f, 1000f))
        assertEquals(400, ReferenceMediaMotion.pageDuration(300f, 300f, 0f, 400f))
        assertEquals(600, ReferenceMediaMotion.pageDuration(300f, 300f, 500f, 400f))
    }

    @Test
    fun smallPicturesToggleBetweenNaturalSizeAndLimitedInitialEnlargement() {
        val small = ImageViewport(400f, 400f, 50f, 50f)
        assertEquals(0.125f, small.minimumScale, 0f)
        assertEquals(1f, small.initialScale, 0f)
        assertEquals(1f, small.maximumScale, 0f)
        val natural = ImageTransform(small.initialScale).doubleTap(200f, 200f, small)
        assertEquals(small.minimumScale, natural.scale, 0f)
        assertEquals(small.initialScale, natural.doubleTap(200f, 200f, small).scale, 0f)
    }

    @Test
    fun nativeSizePictureDoesNotGetAnInventedTripleZoom() {
        val viewport = ImageViewport(300f, 300f, 300f, 300f)
        assertEquals(ImageTransform(), ImageTransform().doubleTap(120f, 120f, viewport))
    }
}
