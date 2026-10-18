package com.solthariel.pawchan

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.dp
import com.solthariel.pawchan.composer.ComposerAttachButton
import com.solthariel.pawchan.composer.ComposerDraftState
import com.solthariel.pawchan.composer.ComposerFieldActions
import com.solthariel.pawchan.composer.ComposerFormattingToolbar
import com.solthariel.pawchan.composer.ComposerTextEditor
import com.solthariel.pawchan.domain.posting.PostingFeatures
import com.solthariel.pawchan.domain.posting.PostingIdentityRules
import com.solthariel.pawchan.domain.posting.PostingRules
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class ComposerEditorUiTest {
    @get:Rule
    val compose = createComposeRule()

    @Test
    fun disabledSubjectAndNamesAreHiddenRatherThanGreyedOut() {
        compose.setContent {
            MaterialTheme {
                ComposerTextEditor(
                    ComposerDraftState(),
                    PostingRules(
                        identity = PostingIdentityRules(namesEnabled = false),
                        features = PostingFeatures(subject = false),
                    ),
                    true,
                    true,
                    ComposerFieldActions({}, {}),
                )
            }
        }
        compose.onNodeWithText("Тема").assertDoesNotExist()
        compose.onNodeWithText("Имя").assertDoesNotExist()
        compose.onNodeWithText("Email").assertIsDisplayed()
    }

    @Test
    fun allFormattingKeysAreVisibleInOneNarrowRow() {
        compose.setContent {
            MaterialTheme {
                Column(Modifier.width(320.dp)) { ComposerFormattingToolbar(true, false, {}, {}) }
            }
        }
        for (label in listOf("B", "I", "U", "O̅", "S", "SUB", "SUP", "SP", ">")) {
            compose.onNodeWithText(label).assertIsDisplayed()
        }
        compose.onNodeWithContentDescription("Удалить всё").assertIsDisplayed()
    }

    @Test fun formattingPressSurfacesAreSquareWithoutHidingActions() {
        compose.setContent {
            MaterialTheme {
                Column(Modifier.width(320.dp)) { ComposerFormattingToolbar(true, false, {}, {}) }
            }
        }
        val surfaces = compose.onAllNodesWithTag("composer-markup-surface", useUnmergedTree = true).fetchSemanticsNodes()
        assertEquals(10, surfaces.size)
        surfaces.forEach { assertTrue(kotlin.math.abs(it.boundsInRoot.width - it.boundsInRoot.height) < 1f) }
    }

    @Test
    fun attachButtonRemainsClickableIndependentlyOfTheAttachmentCount() {
        val clicks = mutableStateOf(0)
        compose.setContent { MaterialTheme { ComposerAttachButton { clicks.value++ } } }
        compose.onNodeWithContentDescription("Прикрепить изображение или видео").performClick().performClick()
        compose.runOnIdle { assertEquals(2, clicks.value) }
    }

    @Test fun clearButtonCallsTheActionImmediatelyWithoutConfirmation() {
        var clicks = 0
        compose.setContent { MaterialTheme { ComposerFormattingToolbar(true, false, { clicks++ }, {}) } }
        compose.onNodeWithContentDescription("Удалить всё").performClick()
        compose.runOnIdle { assertEquals(1, clicks) }
        compose.onNodeWithText("Отмена").assertDoesNotExist()
        compose.onNodeWithText("Удалить всё").assertDoesNotExist()
    }

    @Test fun repeatedFormattingButtonRemovesAnEmptyPairInTheEditor() {
        val draft = mutableStateOf(ComposerDraftState())
        compose.setContent {
            MaterialTheme {
                ComposerTextEditor(
                    draft.value,
                    PostingRules(),
                    true,
                    false,
                    ComposerFieldActions({ draft.value = it }, {}),
                )
            }
        }
        compose.onNodeWithText("B").performClick()
        compose.runOnIdle { assertEquals("[b][/b]", draft.value.comment) }
        compose.onNodeWithText("B").performClick()
        compose.runOnIdle { assertEquals("", draft.value.comment) }
    }
}
