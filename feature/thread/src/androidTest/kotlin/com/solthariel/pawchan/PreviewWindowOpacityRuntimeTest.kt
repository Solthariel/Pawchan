package com.solthariel.pawchan

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.unit.dp
import com.solthariel.pawchan.domain.settings.ThemePreference
import com.solthariel.pawchan.thread.LocalPreviewLayer
import com.solthariel.pawchan.thread.PreviewLayer
import com.solthariel.pawchan.thread.PreviewSwipeState
import com.solthariel.pawchan.thread.PreviewWindowFrame
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class PreviewWindowOpacityRuntimeTest {
    @get:Rule val compose = createComposeRule()

    @Test fun inactiveWindowDoesNotRevealTheThreadBehindIt() {
        val backdrop = mutableStateOf(Color.Red)
        val layer = PreviewLayer(1, PreviewSwipeState(), {})
        compose.setContent {
            ThreadTestTheme(ThemePreference.DARK) {
                Box(Modifier.fillMaxSize().background(backdrop.value)) {
                    CompositionLocalProvider(LocalPreviewLayer provides layer) {
                        PreviewWindowFrame({}) { Box(Modifier.fillMaxWidth().height(160.dp)) }
                    }
                }
            }
        }
        val before = compose.onRoot().captureToImage().toPixelMap()
        val centerBefore = before[before.width / 2, before.height / 2]
        compose.runOnIdle { backdrop.value = Color.Blue }
        val after = compose.onRoot().captureToImage().toPixelMap()
        assertEquals(centerBefore, after[after.width / 2, after.height / 2])
    }
}
