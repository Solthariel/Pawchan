package com.solthariel.pawchan.composer

import com.solthariel.pawchan.domain.drafts.DraftImageEdit
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ImageCropPresetTest {
    @Test
    fun originalFollowsOutputRotation() {
        assertNull(ImageCropPreset.FREE.aspect(DraftImageEdit(), 1600, 900))
        val original = requireNotNull(ImageCropPreset.ORIGINAL.aspect(DraftImageEdit(), 1600, 900))
        assertEquals(1600f / 900f, original, .00001f)
        val rotated = DraftImageEdit(quarterTurns = 1)
        assertEquals(900f / 1600f, requireNotNull(ImageCropPreset.ORIGINAL.aspect(rotated, 1600, 900)), .00001f)
    }

    @Test
    fun rotationInvertsFixedPresetAndSecondRotationRestoresIt() {
        for (preset in ImageCropPreset.entries) assertEquals(preset, preset.rotated().rotated())
        assertEquals(ImageCropPreset.NINE_SIXTEEN, ImageCropPreset.SIXTEEN_NINE.rotated())
    }
}
