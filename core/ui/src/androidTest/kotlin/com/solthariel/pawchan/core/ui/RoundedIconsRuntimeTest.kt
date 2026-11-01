package com.solthariel.pawchan.core.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.dp
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

/** Requires Android resources and rendering; host compilation is not execution. */
class RoundedIconsRuntimeTest {
    @get:Rule val compose = createComposeRule()

    @Test fun everyActionAndSourceMarkerRendersVisibleGeometry() {
        compose.setContent {
            MaterialTheme {
                Column {
                    UiIconKind.entries.chunked(6).forEach { group ->
                        Row {
                            group.forEach { kind ->
                                UiIcon(
                                    kind,
                                    Color.Black,
                                    Modifier.size(24.dp).background(Color.White).testTag(kind.name),
                                )
                            }
                        }
                    }
                }
            }
        }
        UiIconKind.entries.forEach { kind ->
            val pixels = compose.onNodeWithTag(kind.name).captureToImage().toPixelMap()
            val ink =
                (0 until pixels.height).sumOf { y ->
                    (0 until pixels.width).count { x -> pixels[x, y].red < 0.5f }
                }
            assertTrue("Invisible icon: $kind", ink > 0)
        }
    }

    @Test fun galleryUsesCallerTintAndUpdatesWithoutChangingItsBounds() {
        val color = mutableStateOf(Color.Red)
        compose.setContent {
            MaterialTheme {
                UiIcon(
                    UiIconKind.MEDIA_MULTIPLE,
                    color.value,
                    Modifier.size(24.dp).background(Color.White).testTag("gallery"),
                )
            }
        }
        val before = compose.onNodeWithTag("gallery").captureToImage()
        compose.runOnIdle { color.value = Color.Blue }
        val after = compose.onNodeWithTag("gallery").captureToImage()
        assertTrue(before.width == after.width && before.height == after.height)
        val red = before.toPixelMap()
        val blue = after.toPixelMap()
        val tinted =
            (0 until red.height).sumOf { y ->
                (0 until red.width).count { x -> red[x, y].blue < 0.1f && blue[x, y].red < 0.1f }
            }
        assertTrue("Tint was ignored", tinted > 0)
    }
}
