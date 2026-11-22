package com.solthariel.pawchan.composer

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.solthariel.pawchan.domain.drafts.DraftImageEdit
import com.solthariel.pawchan.domain.drafts.DraftPoint
import com.solthariel.pawchan.domain.drafts.DraftStroke
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class ImageCropToolbarRuntimeTest {
    @get:Rule val compose = createComposeRule()

    @Test
    fun cropRatioRotateMirrorAndResetShareHistory() {
        val mark = DraftStroke(listOf(DraftPoint(.5f, .5f)), -1, .1f)
        lateinit var state: ImageEditorState
        compose.setContent {
            MaterialTheme {
                state = remember { ImageEditorState(DraftImageEdit(marks = listOf(mark))) }
                var preset by remember { mutableStateOf(ImageCropPreset.FREE) }
                ImageCropToolbar(state, 1600, 900, preset) { preset = it }
            }
        }
        compose.onNodeWithText("Пропорции: Свободно ▾").performClick()
        compose.onNodeWithText("1:1").performClick()
        compose.runOnIdle {
            val crop = state.edit.crop
            assertEquals(1f, (crop.right - crop.left) * 1600f / ((crop.bottom - crop.top) * 900f), .00001f)
        }
        compose.onNodeWithText("Повернуть").performClick()
        compose.onNodeWithContentDescription("Отразить по горизонтали").performClick()
        compose.runOnIdle { assertTrue(state.edit.mirrored) }
        compose.onNodeWithText("Сбросить").performClick()
        compose.runOnIdle {
            assertEquals(DraftImageEdit(marks = listOf(mark)), state.edit)
            state.undo()
            assertTrue(state.edit.mirrored)
        }
    }
}
