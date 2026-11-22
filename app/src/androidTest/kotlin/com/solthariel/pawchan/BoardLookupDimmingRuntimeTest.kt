package com.solthariel.pawchan

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.dp
import com.solthariel.pawchan.catalog.CatalogBoardLookupBindings
import com.solthariel.pawchan.catalog.CatalogBoardLookupLayer
import com.solthariel.pawchan.core.ui.ModalLayerHost
import com.solthariel.pawchan.core.ui.modalPanelAdditionalBounds
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import kotlin.math.roundToInt

class BoardLookupDimmingRuntimeTest {
    @get:Rule val compose = createComposeRule()

    @Test fun quickPanelStaysUndimmedWhileTheBackgroundDimsAndRestores() {
        val visible = mutableStateOf(true)
        compose.setContent {
            Box(Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.systemBars)) {
                ModalLayerHost {
                    MaterialTheme {
                        CatalogBoardLookupLayer(
                            CatalogBoardLookupBindings(visible.value, "", {}, {}, { visible.value = false }) {
                                Box(Modifier.fillMaxWidth().height(160.dp).background(Color.Blue)) {
                                    TextButton({ visible.value = false }) { Text("Choose board") }
                                }
                            },
                        ) { Box(Modifier.fillMaxSize().background(Color.White)) }
                    }
                }
            }
        }
        val panel = compose.onNodeWithTag("board-lookup-panel").fetchSemanticsNode().boundsInRoot
        val host = compose.onNodeWithTag("modal-layer-host", useUnmergedTree = true)
        val hostBounds = host.fetchSemanticsNode().boundsInRoot
        val pixels = host.captureToImage().toPixelMap()
        assertEquals(0.4f, pixels[pixels.width - 2, pixels.height - 2].red, 0.02f)
        val foreground =
            pixels[
                (panel.center.x - hostBounds.left).roundToInt(),
                (panel.bottom - hostBounds.top - 12).roundToInt(),
            ]
        assertEquals(1f, foreground.blue, 0.02f)
        assertEquals(0f, foreground.red, 0.02f)
        compose.onNodeWithText("Choose board").performClick()
        compose.onNodeWithTag("board-lookup-panel").assertDoesNotExist()
        val restored = host.captureToImage().toPixelMap()
        assertEquals(1f, restored[restored.width - 2, restored.height - 2].red, 0.01f)
    }

    @Test fun lookupToolbarAndSuggestionsAreBothExcludedFromScrim() {
        compose.setContent {
            Box(Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.systemBars)) {
                ModalLayerHost {
                    MaterialTheme {
                        CatalogBoardLookupLayer(
                            CatalogBoardLookupBindings(true, "", {}, {}, {}) {
                                Box(Modifier.fillMaxWidth().height(100.dp).background(Color.Blue))
                            },
                        ) {
                            Box(Modifier.fillMaxSize().background(Color.White)) {
                                Box(
                                    Modifier
                                        .fillMaxWidth()
                                        .height(48.dp)
                                        .modalPanelAdditionalBounds()
                                        .background(Color.Red)
                                        .testTag("lookup-toolbar"),
                                )
                            }
                        }
                    }
                }
            }
        }
        val host = compose.onNodeWithTag("modal-layer-host", useUnmergedTree = true)
        val root = host.fetchSemanticsNode().boundsInRoot
        val toolbar = compose.onNodeWithTag("lookup-toolbar").fetchSemanticsNode().boundsInRoot
        val pixels = host.captureToImage().toPixelMap()
        val pixel = pixels[(toolbar.center.x - root.left).roundToInt(), (toolbar.center.y - root.top).roundToInt()]
        assertEquals(1f, pixel.red, 0.02f)
        assertEquals(0f, pixel.green, 0.02f)
        assertEquals(0.4f, pixels[pixels.width - 2, pixels.height - 2].red, 0.02f)
    }
}
