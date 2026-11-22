package com.solthariel.pawchan.domain.drafts

import org.junit.Assert.assertEquals
import org.junit.Test

class DraftRedactionColorTest {
    @Test fun rectangleAndBrushKeepTheColorChosenWhenTheGestureStarted() {
        DraftRedactionTool.entries.forEach { tool ->
            val selected = DraftMarkTool.Redact(.1f, DraftRedactionMode.FILL, tool, -65536)
            val gesture = requireNotNull(DraftMarkGesture.begin(DraftImageEdit(), selected, DraftPoint(.1f, .1f)))
            val mark = gesture.update(DraftPoint(.8f, .8f)).marks.single() as DraftRedaction
            assertEquals(-65536, mark.color)
        }
    }

    @Test fun fillCannotBecomeTransparentEvenWithAnImportedAlphaChannel() {
        val mark =
            DraftRedaction(
                listOf(DraftPoint(.5f, .5f)),
                DraftRedactionMode.FILL,
                DraftRedactionTool.BRUSH,
                .1f,
                0x00FF0000,
            )
        assertEquals(-65536, mark.opaqueColor)
        assertEquals(-16777216, mark.copy(color = 0).opaqueColor)
    }
}
