package com.solthariel.pawchan

import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import com.solthariel.pawchan.core.ui.postContainerShape
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class PostContainerShapeTest {
    @Test
    fun plainPostIsAnExactRectangle() {
        val outline = postContainerShape().createOutline(Size(100f, 80f), LayoutDirection.Ltr, Density(1f))
        assertTrue(outline is Outline.Rectangle)
        assertEquals(100f, outline.bounds.width)
        assertEquals(80f, outline.bounds.height)
    }

    @Test
    fun directionDoesNotChangeThePostShape() {
        val shape = postContainerShape()
        assertEquals(
            shape.createOutline(Size(100f, 80f), LayoutDirection.Ltr, Density(1f)),
            shape.createOutline(Size(100f, 80f), LayoutDirection.Rtl, Density(1f)),
        )
    }

    @Test
    fun shapesAreReusedAcrossPostStates() {
        assertSame(postContainerShape(), postContainerShape())
    }
}
