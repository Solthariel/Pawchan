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
import com.solthariel.pawchan.domain.drafts.DraftTextAppearance
import com.solthariel.pawchan.domain.drafts.DraftTextMark
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class ImageTextTransformRuntimeTest {
    @get:Rule val compose = createComposeRule()

    @Test
    fun dragMovesSelectedTextAsOneUndoableAction() {
        val mark = DraftTextMark("Текст", DraftPoint(.5f, .5f), DraftTextAppearance(-1, .1f))
        val initial = DraftImageEdit(marks = listOf(mark))
        lateinit var state: ImageEditorState
        compose.setContent {
            MaterialTheme {
                state = remember { ImageEditorState(initial) }
                ImageTextTransformOverlay(state, 0, Modifier.size(300.dp))
            }
        }
        compose.onNodeWithContentDescription("Переместить текст").performTouchInput {
            down(Offset(width * .5f, height * .5f))
            moveTo(Offset(width * .6f, height * .7f))
            up()
        }
        compose.runOnIdle {
            val moved = state.edit.marks.single() as DraftTextMark
            assertEquals(.6f, moved.anchor.x, .01f)
            assertEquals(.7f, moved.anchor.y, .01f)
            state.undo()
            assertEquals(initial, state.edit)
            state.redo()
            assertEquals(moved, state.edit.marks.single())
        }
    }
}
