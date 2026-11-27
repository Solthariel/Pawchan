package com.solthariel.pawchan

import com.solthariel.pawchan.thread.ShotGeometry
import com.solthariel.pawchan.thread.ShotSelection
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.IOException

class ShotSelectionTest {
    private val row = ShotGeometry(100, 8)

    @Test fun nonAdjacentRowsKeepThreadOrderIndependentlyOfTapOrder() {
        val model = ShotSelection<String>()
        for (id in listOf(998L, 995L)) {
            model.select(id, row)
            model.store(id, row, "pixels-$id")
        }
        assertEquals(listOf("pixels-995", "pixels-998"), model.snapshot(setOf(998, 995), listOf(995, 996, 997, 998)))
    }

    @Test fun capturesRemainValidAfterLeavingScreenAndRemovingSelectionDoesNotMutateFrozenRequest() {
        val model = ShotSelection<String>()
        model.select(1, row)
        model.store(1, row, "original")
        val frozen = model.snapshot(setOf(1), listOf(1))
        model.clear()
        assertFalse(model.isReady(setOf(1)))
        assertEquals(listOf("original"), frozen)
    }

    @Test fun incompleteCaptureCannotExportPartOfSelection() {
        val model = ShotSelection<String>()
        model.select(1, row)
        model.store(1, row, "a")
        model.select(2, row)
        assertFalse(model.isReady(setOf(1, 2)))
        assertThrows(IOException::class.java) { model.snapshot(setOf(1, 2), listOf(1, 2)) }
        assertThrows(IOException::class.java) { model.snapshot(emptySet(), listOf(1, 2)) }
    }

    @Test fun disappearedOrDuplicateIdsDoNotProduceAmbiguousImage() {
        val model = ShotSelection<String>()
        model.select(1, row)
        model.store(1, row, "a")
        assertThrows(IOException::class.java) { model.snapshot(setOf(1), emptyList()) }
        assertThrows(IOException::class.java) { model.snapshot(setOf(1), listOf(1, 1)) }
    }

    @Test fun changingWidthNeverStretchesToFitAndDifferentHeightsAreFine() {
        val model = ShotSelection<String>()
        model.select(1, row)
        model.store(1, row, "a")
        val tall = ShotGeometry(100, 44)
        model.select(2, tall)
        model.store(2, tall, "b")
        assertEquals(2, model.snapshot(setOf(1, 2), listOf(1, 2)).size)
        val rotated = ShotGeometry(140, 8)
        model.select(2, rotated)
        model.store(2, rotated, "c")
        assertThrows(IOException::class.java) { model.snapshot(setOf(1, 2), listOf(1, 2)) }
    }

    @Test fun reservesPixelsBeforeReadbackAndReleasesBudgetOnDeselect() {
        val model = ShotSelection<String>(row.pixels)
        model.select(1, row)
        assertThrows(IOException::class.java) { model.select(2, row) }
        model.remove(1)
        model.select(2, row)
        model.store(2, row, "b")
        assertTrue(model.isReady(setOf(2)))
        assertThrows(IOException::class.java) { model.select(3, ShotGeometry(Int.MAX_VALUE, Int.MAX_VALUE)) }
        assertThrows(IOException::class.java) { model.select(3, ShotGeometry(0, 1)) }
    }

    @Test fun lateFrameCannotResurrectRemovedOrResizedRow() {
        val model = ShotSelection<String>()
        model.select(1, row)
        model.remove(1)
        model.store(1, row, "stale")
        assertFalse(model.isReady(setOf(1)))
        model.select(1, ShotGeometry(50, 8))
        model.store(1, row, "old size")
        assertFalse(model.isReady(setOf(1)))
    }
}
