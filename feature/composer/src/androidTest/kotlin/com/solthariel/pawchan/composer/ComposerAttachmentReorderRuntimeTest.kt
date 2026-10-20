package com.solthariel.pawchan.composer

import android.graphics.Bitmap
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.unit.dp
import androidx.test.platform.app.InstrumentationRegistry
import coil3.ImageLoader
import com.solthariel.pawchan.domain.posting.ReplyAttachment
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import java.io.ByteArrayOutputStream

class ComposerAttachmentReorderRuntimeTest {
    @get:Rule val compose = createComposeRule()
    private var files by mutableStateOf(emptyList<ComposerAttachment>())
    private var commits = 0

    @Test fun longPressMovesFirstToLastAndCommitsOnce() {
        show(4)
        drag(cancel = false)
        compose.runOnIdle {
            assertEquals(listOf("1", "2", "3", "0"), files.map { it.id })
            assertEquals(1, commits)
        }
    }

    @Test fun dragCanCrossRowsWithEightAttachments() {
        show(8)
        compose.onNodeWithTag("strip").performTouchInput {
            down(Offset(width / 8f, height / 4f))
            advanceEventTime(800)
            moveTo(Offset(width * 7f / 8f, height * 3f / 4f), delayMillis = 200)
            up()
        }
        compose.runOnIdle {
            assertEquals(listOf("1", "2", "3", "4", "5", "6", "7", "0"), files.map { it.id })
            assertEquals(1, commits)
        }
    }

    @Test fun cancellingDragLeavesOriginalOrder() {
        show(4)
        drag(cancel = true)
        compose.runOnIdle {
            assertEquals(listOf("0", "1", "2", "3"), files.map { it.id })
            assertEquals(0, commits)
        }
    }

    @Test fun disabledAttachmentsCannotBeReordered() {
        show(4, enabled = false)
        drag(cancel = false)
        compose.runOnIdle { assertEquals(0, commits) }
    }

    @Test fun shortTapStillOpensAttachmentMenu() {
        show(4)
        compose.onNodeWithTag("strip").performTouchInput {
            down(Offset(width / 8f, height / 2f))
            advanceEventTime(50)
            up()
        }
        compose.onNodeWithText("Удалить вложение").assertExists()
    }

    private fun show(
        count: Int,
        enabled: Boolean = true,
    ) {
        val image =
            ByteArrayOutputStream().use { output ->
                val bitmap = Bitmap.createBitmap(8, 8, Bitmap.Config.ARGB_8888)
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, output)
                bitmap.recycle()
                output.toByteArray()
            }
        files =
            (0 until count).map {
                ComposerAttachment(ReplyAttachment("$it.png", "image/png", image), id = "$it")
            }
        val loader = ImageLoader(InstrumentationRegistry.getInstrumentation().targetContext)
        compose.setContent {
            MaterialTheme {
                Box(Modifier.width(320.dp).testTag("strip")) {
                    ComposerAttachmentStrip(files, loader, enabled) {
                        commits++
                        files = it
                    }
                }
            }
        }
    }

    private fun drag(cancel: Boolean) {
        compose.onNodeWithTag("strip").performTouchInput {
            down(Offset(width / 8f, height / 2f))
            advanceEventTime(800)
            moveTo(Offset(width * 7f / 8f, height / 2f), delayMillis = 200)
            if (cancel) cancel() else up()
        }
    }
}
