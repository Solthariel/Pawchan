package com.solthariel.pawchan.core.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ItemScrollAnchorsTest {
    @Test fun shortTailDoesNotAllocatePositionsForUnreachablePostStarts() {
        val anchors = ItemScrollAnchors(FastScrollTarget(17, 42))
        assertEquals(18, anchors.lastStep)
        assertEquals(FastScrollTarget(17, 0), anchors.target(17f / 18))
        assertEquals(FastScrollTarget(17, 42), anchors.target(1f))
        assertEquals(FastScrollTarget(16, 0), anchors.target(16f / 18))
    }

    @Test fun everyStepHasADistinctReachableTargetAndUsesTheSameInverse() {
        for (end in listOf(FastScrollTarget(0, 500), FastScrollTarget(17, 42), FastScrollTarget(17, 0))) {
            val anchors = ItemScrollAnchors(end)
            val targets = (0..anchors.lastStep).map { step -> anchors.target(step.toFloat() / anchors.lastStep) }
            assertEquals(targets.size, targets.distinct().size)
            targets.forEach { assertEquals(it, anchors.target(anchors.progress(it.index, it.offset))) }
        }
    }

    @Test fun slowMovementCannotReverseEvenWhenEndIsInsideATallPost() {
        val anchors = ItemScrollAnchors(FastScrollTarget(99, 1600))
        var previous = FastScrollTarget(0, 0)
        repeat(10_001) { step ->
            val target = anchors.target(step / 10_000f)
            assertTrue(
                target.index > previous.index || (target.index == previous.index && target.offset >= previous.offset),
            )
            previous = target
        }
        assertEquals(FastScrollTarget(99, 0), anchors.target(99f / 100))
        assertEquals(FastScrollTarget(99, 1600), anchors.target(1f))
    }

    @Test fun emptyNonScrollableAndInvalidInputsAreSafe() {
        val anchors = ItemScrollAnchors(FastScrollTarget(0, 0))
        for (progress in listOf(0f, 0.5f, 1f, Float.NaN)) assertEquals(FastScrollTarget(0, 0), anchors.target(progress))
        assertEquals(0f, anchors.progress(0, 0))
    }
}
