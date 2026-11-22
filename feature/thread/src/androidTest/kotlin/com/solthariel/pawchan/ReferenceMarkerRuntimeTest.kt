package com.solthariel.pawchan

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.rememberTextMeasurer
import com.solthariel.pawchan.thread.LINK_SUFFIX_TAG
import com.solthariel.pawchan.thread.referenceLinkSuffixes
import com.solthariel.pawchan.thread.withVisualLinkSuffixes
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

/** The visual OP glyph must not alter the source used for text selection/export. */
class ReferenceMarkerRuntimeTest {
    @get:Rule val compose = createComposeRule()

    @Test fun originalPosterIsVisibleWhileSourceTextRemainsUnchanged() {
        val source =
            AnnotatedString
                .Builder(">>42 (OP)")
                .apply {
                    addStringAnnotation(LINK_SUFFIX_TAG, "same-thread", 5, 9)
                }.toAnnotatedString()
        var measured: TextLayoutResult? = null
        compose.setContent {
            MaterialTheme {
                var layout by remember { mutableStateOf<TextLayoutResult?>(null) }
                val measurer = rememberTextMeasurer()
                val style = MaterialTheme.typography.bodyLarge
                val modifier =
                    Modifier.testTag("reference").referenceLinkSuffixes(
                        source,
                        null,
                        measurer,
                        style,
                        Color.Blue,
                    ) { layout }
                Text(source.withVisualLinkSuffixes(), modifier, color = Color.Blue, style = style, onTextLayout = {
                    layout = it
                    measured = it
                })
            }
        }
        val pixels = compose.onNodeWithTag("reference").captureToImage().toPixelMap()
        val result = requireNotNull(measured)
        assertEquals(source.text, result.layoutInput.text.text)
        val left =
            result
                .getBoundingBox(5)
                .left
                .toInt()
                .coerceAtLeast(0)
        val ink =
            (0 until pixels.height).sumOf { y ->
                (left until pixels.width).count { x ->
                    val pixel = pixels[x, y]
                    pixel.alpha > 0.5f && pixel.blue > pixel.red
                }
            }
        assertTrue("OP label is invisible", ink > 0)
    }
}
