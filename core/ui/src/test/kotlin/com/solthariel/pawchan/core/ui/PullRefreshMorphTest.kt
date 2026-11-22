package com.solthariel.pawchan.core.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.PI

class PullRefreshMorphTest {
    @Test
    fun fingerDistanceControlsMorphAndClampsAtLoadingEndpoint() {
        assertEquals(0f, pullRefreshMorphPhase(-1f), 0f)
        assertEquals(0.25f, pullRefreshMorphPhase(0.25f), 0f)
        assertEquals(0.5f, pullRefreshMorphPhase(0.5f), 0f)
        assertEquals(1f, pullRefreshMorphPhase(1f), 0f)
        assertEquals(1f, pullRefreshMorphPhase(2f), 0f)
        for (step in 0..64) {
            val angle = step * 2.0 * PI / 64.0
            assertEquals(refreshMorphRadius(angle, pullRefreshMorphPhase(1f)), refreshMorphRadius(angle, 0f), 0.00001f)
        }
    }

    @Test
    fun shapeFitsInDiscAndChangesSmoothlyWithMatchingEndpoints() {
        for (step in 0..64) {
            val angle = step * 2.0 * PI / 64.0
            assertEquals(refreshMorphRadius(angle, 0f), refreshMorphRadius(angle, 1f), 0.00001f)
            for (phase in 0..100) {
                assertTrue(refreshMorphRadius(angle, phase / 100f) in 0.75999f..1.00001f)
            }
        }
    }

    @Test
    fun shapeMorphsBetweenWavyAndFourSidedForms() {
        val angle = PI / 8.0
        assertTrue(refreshMorphRadius(angle, 0.5f) > refreshMorphRadius(angle, 0f))
    }
}
