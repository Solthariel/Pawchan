package com.solthariel.pawchan.composer

import com.solthariel.pawchan.domain.drafts.CanvasCrop
import com.solthariel.pawchan.domain.drafts.DraftCropHandle
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ImageCropHitTest {
    private val crop = CanvasCrop(0.2f, 0.3f, 0.8f, 0.7f)

    @Test
    fun edgesAndCornersHaveGenerousTouchTargets() {
        assertEquals(DraftCropHandle.TOP_LEFT, cropHandleAt(crop, 1000f, 500f, 210f, 155f, 24f))
        assertEquals(DraftCropHandle.BOTTOM_RIGHT, cropHandleAt(crop, 1000f, 500f, 790f, 345f, 24f))
        assertEquals(DraftCropHandle.LEFT, cropHandleAt(crop, 1000f, 500f, 190f, 240f, 24f))
        assertEquals(DraftCropHandle.TOP, cropHandleAt(crop, 1000f, 500f, 500f, 140f, 24f))
        assertNull(cropHandleAt(crop, 1000f, 500f, 500f, 250f, 24f))
        assertNull(cropHandleAt(crop, 1000f, 500f, 100f, 150f, 24f))
    }

    @Test
    fun selectedHandleUsesExactEdgeEvenIfTouchedNearby() {
        val corner = cropHandlePosition(crop, DraftCropHandle.TOP_LEFT)
        assertEquals(0.2f, corner.x, 0.00001f)
        assertEquals(0.3f, corner.y, 0.00001f)
        val edge = cropHandlePosition(crop, DraftCropHandle.RIGHT)
        assertEquals(0.8f, edge.x, 0.00001f)
        assertEquals(0.5f, edge.y, 0.00001f)
    }
}
