package com.solthariel.pawchan.composer

import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.unit.dp
import com.solthariel.pawchan.domain.drafts.DraftImageEdit
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class ImageCropOverlayRuntimeTest {
    @get:Rule val compose = createComposeRule()

    @Test
    fun draggingCornerEmitsOneGestureAndChangesCrop() {
        var edit by mutableStateOf(DraftImageEdit())
        var starts = 0
        var finishes = 0
        compose.setContent {
            MaterialTheme {
                ImagePaddedCropOverlay(
                    edit,
                    ImageCropGeometry(1000, 1000, null),
                    ImageCropActions({ starts++ }, { edit = it }, { finishes++ }),
                    Modifier.size(300.dp, 200.dp),
                )
            }
        }
        compose.onNodeWithContentDescription("Рамка обрезки").performTouchInput {
            down(Offset(1f, 1f))
            moveTo(Offset(width * 0.25f, height * 0.25f))
            up()
        }
        compose.runOnIdle {
            assertEquals(1, starts)
            assertEquals(1, finishes)
            assertTrue(edit.crop.left > 0.2f && edit.crop.left < 0.3f)
            assertTrue(edit.crop.top > 0.2f && edit.crop.top < 0.3f)
            assertEquals(1f, edit.crop.right, 0.00001f)
            assertEquals(1f, edit.crop.bottom, 0.00001f)
        }
    }

    @Test
    fun cornerCanBeGrabbedFromPaddingOutsideImage() {
        var edit by mutableStateOf(DraftImageEdit())
        var starts = 0
        compose.setContent {
            MaterialTheme {
                ImagePaddedCropOverlay(
                    edit,
                    ImageCropGeometry(1000, 1000, null, 24.dp),
                    ImageCropActions({ starts++ }, { edit = it }, {}),
                    Modifier.size(248.dp),
                )
            }
        }
        compose.onNodeWithContentDescription("Рамка обрезки").performTouchInput {
            // 12 dp from the outer corner lies outside the image, within the 24 dp touch radius.
            down(Offset(width * 12f / 248f, height * 12f / 248f))
            moveTo(Offset(width * 62f / 248f, height * 62f / 248f))
            up()
        }
        compose.runOnIdle {
            assertEquals(1, starts)
            assertTrue(edit.crop.left > 0.2f)
            assertTrue(edit.crop.top > 0.2f)
        }
    }

    @Test
    fun secondFingerStopsTheCropGesture() {
        var edit by mutableStateOf(DraftImageEdit())
        var finished = 0
        compose.setContent {
            MaterialTheme {
                ImagePaddedCropOverlay(
                    edit,
                    ImageCropGeometry(1000, 1000, null),
                    ImageCropActions({}, { edit = it }, { finished++ }),
                    Modifier.size(300.dp, 200.dp),
                )
            }
        }
        compose.onNodeWithContentDescription("Рамка обрезки").performTouchInput {
            down(0, Offset(1f, 1f))
            down(1, Offset(width / 2f, height / 2f))
            moveTo(0, Offset(width / 4f, height / 4f))
            up(0)
            up(1)
        }
        compose.runOnIdle {
            assertEquals(DraftImageEdit(), edit)
            assertEquals(1, finished)
        }
    }
}
