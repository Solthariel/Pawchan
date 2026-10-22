package com.solthariel.pawchan

import com.solthariel.pawchan.media.video.VideoPipPrivacy
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class VideoPipPrivacyTest {
    @Test
    fun unlockedProtectedHostCanEnterButLockRevokesPermission() {
        var unlocked = true
        var closes = 0
        val privacy = VideoPipPrivacy({ unlocked }, { closes += 1 })
        assertTrue(privacy.allowed())
        unlocked = false
        assertFalse(privacy.allowed())
        privacy.revoke()
        assertEquals(1, closes)
    }

    @Test
    fun staleControlOrLayoutCannotReopenARevokedWindow() {
        var unlocked = true
        var closes = 0
        val privacy = VideoPipPrivacy({ unlocked }, { closes += 1 })
        privacy.revoke()
        unlocked = false
        privacy.revoke()
        unlocked = true
        assertFalse(privacy.allowed())
        privacy.revoke()
        assertEquals(1, closes)
    }

    @Test
    fun deniedHostCannotEnterInTheFirstPlace() {
        val privacy = VideoPipPrivacy({ false }, {})
        assertFalse(privacy.allowed())
    }
}
