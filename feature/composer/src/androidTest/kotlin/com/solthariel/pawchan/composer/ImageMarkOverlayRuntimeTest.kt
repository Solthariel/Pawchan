package com.solthariel.pawchan.composer

import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.unit.dp
import com.solthariel.pawchan.domain.drafts.DraftImageEdit
import com.solthariel.pawchan.domain.drafts.DraftMarkTool
import com.solthariel.pawchan.domain.drafts.DraftStroke
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class ImageMarkOverlayRuntimeTest {
    @get:Rule val compose = createComposeRule()

    @Test
    fun strokeIsOneUndoStep() {
        lateinit var state: ImageEditorState
        compose.setContent {
            MaterialTheme {
                state = remember { ImageEditorState(DraftImageEdit()) }
                ImageMarkOverlay(state.edit, DraftMarkTool.Pen(-1, .1f), state.markActions({}), Modifier.size(300.dp))
            }
        }
        compose.onNodeWithContentDescription("Поле рисования").performTouchInput {
            down(Offset(width * .1f, height * .2f))
            moveTo(Offset(width * .7f, height * .8f))
            up()
        }
        compose.runOnIdle {
            assertTrue((state.edit.marks.single() as DraftStroke).points.size >= 2)
            state.undo()
            assertTrue(state.edit.marks.isEmpty())
            state.redo()
            assertEquals(1, state.edit.marks.size)
        }
    }

    @Test
    fun secondFingerCancelsUnfinishedStroke() {
        lateinit var state: ImageEditorState
        compose.setContent {
            MaterialTheme {
                state = remember { ImageEditorState(DraftImageEdit()) }
                ImageMarkOverlay(state.edit, DraftMarkTool.Pen(-1, .1f), state.markActions({}), Modifier.size(300.dp))
            }
        }
        compose.onNodeWithContentDescription("Поле рисования").performTouchInput {
            down(0, Offset(width * .1f, height * .2f))
            down(1, Offset(width * .7f, height * .8f))
            up(0)
            up(1)
        }
        compose.runOnIdle {
            assertTrue(state.edit.marks.isEmpty())
            assertTrue(!state.inGesture)
        }
    }
}
