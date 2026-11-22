package com.solthariel.pawchan

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.solthariel.pawchan.core.ui.APP_TOP_BAR_HEIGHT
import com.solthariel.pawchan.core.ui.FloatingTopChrome
import com.solthariel.pawchan.core.ui.LocalFloatingTopEnabled
import com.solthariel.pawchan.core.ui.LocalFloatingTopStatusInset
import com.solthariel.pawchan.core.ui.LocalTopOverlayInset
import com.solthariel.pawchan.core.ui.rememberFloatingTopChromeState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class FloatingTopChromeSwitchRuntimeTest {
    @get:Rule val compose = createComposeRule()

    @Test fun twoRowInsetDoesNotResetWhenForumChanges() {
        var site by mutableIntStateOf(0)
        val observed = mutableListOf<Pair<Int, Dp>>()
        compose.setContent {
            MaterialTheme {
                val chrome = rememberFloatingTopChromeState(APP_TOP_BAR_HEIGHT + 56.dp)
                CompositionLocalProvider(
                    LocalFloatingTopEnabled provides true,
                    LocalFloatingTopStatusInset provides 20.dp,
                ) {
                    key(site) {
                        FloatingTopChrome(toolbar = { Box(Modifier.fillMaxWidth().height(120.dp)) }, state = chrome) {
                            val inset = LocalTopOverlayInset.current
                            SideEffect { observed.add(site to inset) }
                        }
                    }
                }
            }
        }
        compose.waitForIdle()
        listOf(1, 2, 0).forEach { next ->
            compose.runOnIdle { site = next }
            compose.waitForIdle()
        }
        compose.runOnIdle {
            assertTrue(observed.map { it.first }.containsAll(listOf(0, 1, 2)))
            observed.forEach { (_, inset) -> assertEquals(140f, inset.value, 0.8f) }
        }
    }

    @Test fun toolbarStillRespondsToHeightChanges() {
        var height by mutableStateOf(120.dp)
        var observed = 0.dp
        compose.setContent {
            MaterialTheme {
                val chrome = rememberFloatingTopChromeState(APP_TOP_BAR_HEIGHT + 56.dp)
                CompositionLocalProvider(
                    LocalFloatingTopEnabled provides true,
                    LocalFloatingTopStatusInset provides 20.dp,
                ) {
                    FloatingTopChrome(toolbar = { Box(Modifier.fillMaxWidth().height(height)) }, state = chrome) {
                        val inset = LocalTopOverlayInset.current
                        SideEffect { observed = inset }
                    }
                }
            }
        }
        compose.runOnIdle { height = 160.dp }
        compose.waitForIdle()
        compose.runOnIdle { assertEquals(180f, observed.value, 0.8f) }
    }
}
