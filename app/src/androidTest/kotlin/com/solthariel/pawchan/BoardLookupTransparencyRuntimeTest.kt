package com.solthariel.pawchan

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import com.solthariel.pawchan.catalog.CatalogBoardLookupBindings
import com.solthariel.pawchan.catalog.CatalogBoardLookupToolbar
import com.solthariel.pawchan.core.ui.FloatingTopChrome
import com.solthariel.pawchan.core.ui.LocalFloatingTopEnabled
import com.solthariel.pawchan.core.ui.LocalTopBarTransparency
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class BoardLookupTransparencyRuntimeTest {
    @get:Rule val compose = createComposeRule()

    @Test fun lookupIsOpaqueAtEveryTransparencyAndNormalToolbarRetainsTheSetting() {
        val transparency = mutableStateOf(0)
        val opaque = mutableStateOf(true)
        val scheme = mutableStateOf(darkColorScheme())
        compose.setContent {
            MaterialTheme(colorScheme = scheme.value) {
                CompositionLocalProvider(LocalFloatingTopEnabled provides true, LocalTopBarTransparency provides transparency.value) {
                    FloatingTopChrome(
                        opaque = opaque.value,
                        toolbar = { CatalogBoardLookupToolbar(CatalogBoardLookupBindings(true, "", {}, {}, {}, suggestions = {})) },
                    ) { Box(Modifier.fillMaxSize().background(Color.Magenta)) }
                }
            }
        }
        for (theme in listOf(darkColorScheme(), lightColorScheme())) {
            for (percent in listOf(0, 50, 100)) {
                compose.runOnIdle {
                    scheme.value = theme
                    transparency.value = percent
                }
                assertToolbarColor(theme.background)
            }
        }
        compose.runOnIdle { opaque.value = false }
        assertToolbarColor(Color.Magenta)
    }

    private fun assertToolbarColor(expected: Color) {
        val pixels = compose.onNodeWithTag("board-lookup-toolbar").captureToImage().toPixelMap()
        val actual = pixels[1, pixels.height / 2]
        assertEquals(expected.red, actual.red, 0.01f)
        assertEquals(expected.green, actual.green, 0.01f)
        assertEquals(expected.blue, actual.blue, 0.01f)
    }
}
