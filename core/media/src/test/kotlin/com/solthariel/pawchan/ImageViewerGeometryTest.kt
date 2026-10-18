package com.solthariel.pawchan

import com.solthariel.pawchan.media.ImageGestureSample
import com.solthariel.pawchan.media.ImageTransform
import com.solthariel.pawchan.media.ImageViewport
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ImageViewerGeometryTest {
    private val viewport = ImageViewport(300f, 300f, 300f, 300f)

    @Test
    fun zoomKeepsTheImagePointUnderTheFinger() {
        val actual = ImageTransform().zoom(ImageGestureSample(0f, 0f, 2f, 200f, 150f, 2), viewport)
        assertEquals(2f, actual.scale, 0f)
        assertEquals(-50f, actual.x, 0f)
        assertEquals(0f, actual.y, 0f)
    }

    @Test
    fun panIsBoundedByRealFittedImageDimensions() {
        val wide = ImageViewport(300f, 600f, 1200f, 600f)
        val actual = ImageTransform(2f, 500f, 500f).bounded(wide)
        assertEquals(150f, actual.x, 0f)
        assertEquals(0f, actual.y, 0f)
    }

    @Test
    fun pinchCanUnderscaleToHalfMinimumAndRespectsNativeMaximum() {
        val smaller = ImageTransform(2f, 50f, 50f).zoom(ImageGestureSample(0f, 0f, 0.1f, 150f, 150f, 2), viewport)
        assertEquals(ImageTransform(0.5f), smaller)
        val bigger = ImageTransform().zoom(ImageGestureSample(0f, 0f, 100f, 150f, 150f, 2), viewport)
        assertEquals(viewport.maximumScale, bigger.scale, 0f)
    }

    @Test
    fun doubleTapZoomsAtTheTapAndSecondTapResets() {
        val largeImage = ImageViewport(300f, 300f, 600f, 600f)
        val zoomed = ImageTransform().doubleTap(220f, 200f, largeImage)
        assertTrue(zoomed.scale > 1f)
        assertTrue(zoomed.x < 0f && zoomed.y < 0f)
        assertEquals(ImageTransform(), zoomed.doubleTap(220f, 200f, largeImage))
    }
}
