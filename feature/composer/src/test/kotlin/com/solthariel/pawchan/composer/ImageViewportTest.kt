package com.solthariel.pawchan.composer

import org.junit.Assert.assertEquals
import org.junit.Test

class ImageViewportTest {
    @Test
    fun pinchKeepsFocusAndClampsScaleAndPan() {
        val motion = ImageViewportMotion(2f, 0f, 0f, 20f, -10f, 100f, 100f)
        val zoomed = ImageViewport().transform(motion)
        assertEquals(ImageViewport(2f, -20f, 10f), zoomed)
        val panned = zoomed.transform(motion.copy(zoom = 100f, panX = 10000f, panY = -10000f))
        assertEquals(ImageViewport(8f, 350f, -350f), panned)
        assertEquals(ImageViewport(), panned.transform(motion.copy(zoom = .001f)))
    }

    @Test
    fun invalidInputCannotCorruptViewport() {
        val viewport = ImageViewport(2f, 10f, 20f)
        val motion = ImageViewportMotion(1f, 0f, 0f, 0f, 0f, 100f, 100f)
        assertEquals(viewport, viewport.transform(motion.copy(zoom = Float.NaN)))
        assertEquals(viewport, viewport.transform(motion.copy(zoom = 0f)))
        assertEquals(viewport, viewport.transform(motion.copy(width = 0f)))
        assertEquals(viewport, viewport.transform(motion.copy(panY = Float.POSITIVE_INFINITY)))
    }
}
