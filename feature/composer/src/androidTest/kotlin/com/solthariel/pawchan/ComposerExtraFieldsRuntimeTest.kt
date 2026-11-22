package com.solthariel.pawchan

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.text.TextLayoutResult
import com.solthariel.pawchan.composer.ComposerCaptchaState
import com.solthariel.pawchan.composer.ComposerDraftState
import com.solthariel.pawchan.composer.ComposerFailureState
import com.solthariel.pawchan.composer.ComposerFieldsAccess
import com.solthariel.pawchan.composer.ComposerFieldsButton
import com.solthariel.pawchan.composer.ComposerFieldsDialog
import com.solthariel.pawchan.composer.ComposerProgressState
import com.solthariel.pawchan.composer.ComposerScreenState
import com.solthariel.pawchan.composer.ComposerScreenStatus
import com.solthariel.pawchan.composer.availableComposerFields
import com.solthariel.pawchan.composer.filledComposerFieldCount
import com.solthariel.pawchan.domain.posting.PostingRules
import com.solthariel.pawchan.domain.settings.ComposerField
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class ComposerExtraFieldsRuntimeTest {
    @get:Rule val compose = createComposeRule()

    @Test fun headingIsCenteredAndCountersAreAlignedToTheEnd() {
        compose.setContent {
            MaterialTheme {
                ComposerFieldsDialog(
                    ComposerScreenState(
                        ComposerDraftState(),
                        PostingRules(),
                        ComposerCaptchaState(),
                        ComposerScreenStatus(ComposerProgressState(), ComposerFailureState(), false),
                    ),
                    listOf(ComposerField.SUBJECT, ComposerField.TAG),
                    {},
                    {},
                )
            }
        }
        val surface = compose.onNodeWithTag("composer-modal-surface").fetchSemanticsNode().boundsInRoot
        val headingBounds =
            compose.onNodeWithText("Дополнительные поля", useUnmergedTree = true).fetchSemanticsNode().boundsInRoot
        assertEquals(surface.center.x, headingBounds.center.x, compose.density.density)
        val heading = textLayout("Дополнительные поля")
        assertEquals(heading.size.width / 2f, (heading.getLineLeft(0) + heading.getLineRight(0)) / 2f, 2f)
        for (counter in listOf("0 / 100", "0 / 8")) {
            val bounds = compose.onNodeWithText(counter, useUnmergedTree = true).fetchSemanticsNode().boundsInRoot
            assertEquals(32f * compose.density.density, surface.right - bounds.right, 3f * compose.density.density)
            val layout = textLayout(counter)
            assertEquals(layout.size.width.toFloat(), layout.getLineRight(0), 2f)
        }
    }

    private fun textLayout(text: String): TextLayoutResult {
        val layouts = mutableListOf<TextLayoutResult>()
        compose
            .onNodeWithText(text, useUnmergedTree = true)
            .performSemanticsAction(SemanticsActions.GetTextLayoutResult) { it(layouts) }
        return layouts.single()
    }

    @Test fun clearUpdatesTheBadgeAndPreservesTheComment() {
        val draft = mutableStateOf(ComposerDraftState(comment = "keep", email = "mail"))
        val shown = mutableStateOf(false)
        val rules = PostingRules()
        val fields = availableComposerFields(rules, false, emptySet())
        compose.setContent {
            MaterialTheme {
                ComposerFieldsButton(ComposerFieldsAccess(filledComposerFieldCount(draft.value, fields)) { shown.value = true }, true)
                if (shown.value) {
                    ComposerFieldsDialog(
                        ComposerScreenState(
                            draft.value,
                            rules,
                            ComposerCaptchaState(),
                            ComposerScreenStatus(ComposerProgressState(), ComposerFailureState(), false),
                        ),
                        fields,
                        { draft.value = it },
                        { shown.value = false },
                    )
                }
            }
        }
        compose.onNodeWithContentDescription("Дополнительные поля").performClick()
        compose.onNodeWithText("Email").assertIsDisplayed()
        compose.onNodeWithContentDescription("Очистить: Email").performClick()
        compose.runOnIdle {
            assertEquals("", draft.value.email)
            assertEquals("keep", draft.value.comment)
        }
        compose.onNodeWithText("Готово").performClick()
        compose.onNodeWithContentDescription("Дополнительные поля").assert(
            androidx.compose.ui.test.SemanticsMatcher.expectValue(
                androidx.compose.ui.semantics.SemanticsProperties.StateDescription,
                "Заполнено 0",
            ),
        )
        compose.onNodeWithText("1").assertDoesNotExist()
    }
}
