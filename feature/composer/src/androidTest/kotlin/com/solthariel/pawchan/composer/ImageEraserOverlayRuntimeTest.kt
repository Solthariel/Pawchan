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
import com.solthariel.pawchan.domain.drafts.DraftPoint
import com.solthariel.pawchan.domain.drafts.DraftStroke
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class ImageEraserOverlayRuntimeTest {
    @get:Rule val compose = createComposeRule()

    @Test
    fun dragErasesEntireStrokeAndUndoRestoresIt() {
        val mark = DraftStroke(listOf(DraftPoint(.2f, .5f), DraftPoint(.8f, .5f)), -1, .03f)
        lateinit var state: ImageEditorState
        compose.setContent {
            MaterialTheme {
                state = remember { ImageEditorState(DraftImageEdit(marks = listOf(mark))) }
                ImageEraserOverlay(state.edit, 100, 100, state.markActions({}), Modifier.size(300.dp))
            }
        }
        compose.onNodeWithContentDescription("Поле ластика").performTouchInput {
            down(Offset(width * .5f, height * .5f))
            up()
        }
        compose.runOnIdle {
            assertTrue(state.edit.marks.isEmpty())
            state.undo()
            assertEquals(listOf(mark), state.edit.marks)
        }
    }

    @Test
    fun secondFingerCancelsErase() {
        val mark = DraftStroke(listOf(DraftPoint(.5f, .5f)), -1, .03f)
        lateinit var state: ImageEditorState
        compose.setContent {
            MaterialTheme {
                state = remember { ImageEditorState(DraftImageEdit(marks = listOf(mark))) }
                ImageEraserOverlay(state.edit, 100, 100, state.markActions({}), Modifier.size(300.dp))
            }
        }
        compose.onNodeWithContentDescription("Поле ластика").performTouchInput {
            down(0, Offset(width * .5f, height * .5f))
            down(1, Offset(width * .8f, height * .8f))
            up(0)
            up(1)
        }
        compose.runOnIdle {
            assertEquals(listOf(mark), state.edit.marks)
            assertTrue(!state.inGesture)
        }
    }
}
