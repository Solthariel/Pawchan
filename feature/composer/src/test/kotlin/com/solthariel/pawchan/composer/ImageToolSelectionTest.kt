package com.solthariel.pawchan.composer

import com.solthariel.pawchan.domain.drafts.DraftMarkTool
import com.solthariel.pawchan.domain.drafts.DraftRedactionMode
import com.solthariel.pawchan.domain.drafts.DraftRedactionTool
import com.solthariel.pawchan.domain.drafts.DraftShapeKind
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ImageToolSelectionTest {
    @Test fun redactionColorIsIndependentFromDrawingAndPersistsWhenChangingItsMode() {
        val tools = ImageToolSelection()
        tools.color = -1
        tools.redactionColor = -65536
        assertEquals(-65536, (tools.redactionConfig().markTool as DraftMarkTool.Redact).color)
        tools.redactionMode = DraftRedactionMode.PIXELS
        tools.redactionMode = DraftRedactionMode.FILL
        assertEquals(-65536, (tools.redactionConfig().markTool as DraftMarkTool.Redact).color)
        assertEquals(-1, tools.color)
    }

    @Test
    fun selectedDrawingToolBecomesCanvasAction() {
        val tools = ImageToolSelection()
        assertEquals(
            DraftMarkTool.Pen(IMAGE_EDITOR_COLORS.first(), DEFAULT_MARK_WIDTH),
            tools.drawingConfig(40, 20).markTool,
        )
        tools.drawing = ImageDrawingTool.MARKER
        assertTrue((tools.drawingConfig(40, 20).markTool as DraftMarkTool.Pen).marker)
        tools.drawing = ImageDrawingTool.ARROW
        assertEquals(DraftShapeKind.ARROW, (tools.drawingConfig(40, 20).markTool as DraftMarkTool.Shape).kind)
        tools.drawing = ImageDrawingTool.ERASER
        assertEquals(ImageEditorCanvasMode.ERASE, tools.drawingConfig(40, 20).mode)
        assertEquals(40, tools.drawingConfig(40, 20).eraser?.width)
        tools.width = .08f
        assertEquals(.04f, requireNotNull(tools.drawingConfig(40, 20).eraser).radius, .0001f)
    }

    @Test
    fun redactionDefaultsToSolidBrushAndCanChange() {
        val tools = ImageToolSelection()
        assertEquals(
            DraftMarkTool.Redact(DEFAULT_MARK_WIDTH, DraftRedactionMode.FILL, DraftRedactionTool.BRUSH),
            tools.redactionConfig().markTool,
        )
        tools.redactionMode = DraftRedactionMode.PIXELS
        tools.redactionTool = DraftRedactionTool.RECTANGLE
        assertEquals(
            DraftMarkTool.Redact(DEFAULT_MARK_WIDTH, DraftRedactionMode.PIXELS, DraftRedactionTool.RECTANGLE),
            tools.redactionConfig().markTool,
        )
        assertEquals(8, IMAGE_EDITOR_COLORS.size)
    }
}
