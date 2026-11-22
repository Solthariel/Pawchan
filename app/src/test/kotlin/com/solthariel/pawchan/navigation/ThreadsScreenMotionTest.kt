package com.solthariel.pawchan.navigation

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ThreadsScreenMotionTest {
    @Test fun `only hub and its three child routes animate in their correct direction`() {
        THREAD_DETAIL_ROUTES.forEach { route ->
            assertEquals(true, threadsScreenMotion(AppRoute.LIBRARY, route))
            assertEquals(false, threadsScreenMotion(route, AppRoute.LIBRARY))
        }
        assertNull(threadsScreenMotion(AppRoute.HISTORY, AppRoute.COPIES))
        assertNull(threadsScreenMotion(AppRoute.LIBRARY, AppRoute.SETTINGS))
        assertNull(threadsScreenMotion(null, AppRoute.LIBRARY))
    }
}
