package com.solthariel.pawchan

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.platform.LocalTextToolbar
import androidx.compose.ui.platform.TextToolbar
import androidx.compose.ui.platform.TextToolbarStatus
import androidx.compose.ui.test.click
import androidx.compose.ui.test.doubleClick
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import com.solthariel.pawchan.domain.model.PostDocument
import com.solthariel.pawchan.domain.settings.ThemePreference
import com.solthariel.pawchan.thread.PostSelectionHost
import com.solthariel.pawchan.thread.RichPostText
import com.solthariel.pawchan.thread.RichPostTextUi
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class PostSelectionDismissRuntimeTest {
    @get:Rule val compose = createComposeRule()

    @Test
    fun outsideTapAndDisposalHideParagraphMenu() {
        val toolbar = RecordingTextToolbar()
        val model = RichPostTextUi.create(PostDocument.plain("Текст для выделения"))
        var visible by mutableStateOf(true)
        compose.setContent {
            CompositionLocalProvider(LocalTextToolbar provides toolbar) {
                ThreadTestTheme(ThemePreference.DARK) {
                    PostSelectionHost {
                        Column {
                            if (visible) RichPostText(model, {})
                            TextButton(onClick = {}) { Text("Снаружи") }
                            TextButton(onClick = { visible = false }) { Text("Убрать пост") }
                        }
                    }
                }
            }
        }
        compose.onNodeWithText("Текст для выделения").performTouchInput { doubleClick(center) }
        compose.runOnIdle { assertEquals(TextToolbarStatus.Shown, toolbar.status) }
        compose.onNodeWithText("Снаружи").performTouchInput { click(center) }
        compose.runOnIdle { assertEquals(TextToolbarStatus.Hidden, toolbar.status) }
        compose.onNodeWithText("Текст для выделения").performTouchInput { doubleClick(center) }
        compose.runOnIdle { assertEquals(TextToolbarStatus.Shown, toolbar.status) }
        compose.onNodeWithText("Убрать пост").performClick()
        compose.runOnIdle { assertEquals(TextToolbarStatus.Hidden, toolbar.status) }
    }
}

private class RecordingTextToolbar : TextToolbar {
    override var status = TextToolbarStatus.Hidden
        private set

    override fun showMenu(
        rect: Rect,
        onCopyRequested: (() -> Unit)?,
        onPasteRequested: (() -> Unit)?,
        onCutRequested: (() -> Unit)?,
        onSelectAllRequested: (() -> Unit)?,
    ) {
        status = TextToolbarStatus.Shown
    }

    override fun hide() {
        status = TextToolbarStatus.Hidden
    }
}
