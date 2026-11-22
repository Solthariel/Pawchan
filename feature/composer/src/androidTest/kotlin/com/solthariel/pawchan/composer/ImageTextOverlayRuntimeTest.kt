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
import com.solthariel.pawchan.domain.drafts.DraftTextAppearance
import com.solthariel.pawchan.domain.drafts.DraftTextMark
import com.solthariel.pawchan.domain.drafts.addTextMark
import com.solthariel.pawchan.domain.drafts.sourceToCanvas
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class ImageTextOverlayRuntimeTest {
    @get:Rule val compose = createComposeRule()

    @Test
    fun tapAddsTextAtPositionAndUndoRemovesIt() {
        lateinit var state: ImageEditorState
        compose.setContent {
            MaterialTheme {
                state = remember { ImageEditorState(DraftImageEdit(quarterTurns = 1, mirrored = true)) }
                ImageTextOverlay(onPlace = { point ->
                    val appearance = DraftTextAppearance(color = -1, size = .1f)
                    state.commit(requireNotNull(state.edit.addTextMark(point, "Текст", appearance)))
                }, modifier = Modifier.size(300.dp))
            }
        }
        compose.onNodeWithContentDescription("Поле текста").performTouchInput {
            down(Offset(width * .25f, height * .75f))
            up()
        }
        compose.runOnIdle {
            val mark = state.edit.marks.single() as DraftTextMark
            val point = state.edit.sourceToCanvas(mark.anchor)
            assertEquals(.25f, point.x, .01f)
            assertEquals(.75f, point.y, .01f)
            state.undo()
            assertTrue(state.edit.marks.isEmpty())
        }
    }
}
