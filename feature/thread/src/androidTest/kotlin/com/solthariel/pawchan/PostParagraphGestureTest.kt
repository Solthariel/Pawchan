package com.solthariel.pawchan

import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.doubleClick
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.longClick
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.text.TextRange
import com.solthariel.pawchan.domain.model.PostDocument
import com.solthariel.pawchan.thread.RichPostPresentation
import com.solthariel.pawchan.thread.RichPostText
import com.solthariel.pawchan.thread.RichPostTextUi
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class PostParagraphGestureTest {
    @get:Rule val rule = createComposeRule()

    @Test fun doubleTapSelectsParagraphRatherThanWord() {
        val text = "one two three"
        val model = RichPostTextUi.create(PostDocument.plain(text))
        rule.setContent {
            ThreadTestTheme(com.solthariel.pawchan.domain.settings.ThemePreference.SYSTEM) { RichPostText(model, {}) }
        }
        rule.onNodeWithText(text).performTouchInput { doubleClick(center) }
        rule
            .onNodeWithText(
                text,
            ).assert(SemanticsMatcher.expectValue(SemanticsProperties.TextSelectionRange, TextRange(0, text.length)))
    }

    @Test fun longPressInvokesPostMenu() {
        var menu = false
        val model = RichPostTextUi.create(PostDocument.plain("post text"))
        rule.setContent {
            ThreadTestTheme(com.solthariel.pawchan.domain.settings.ThemePreference.SYSTEM) {
                RichPostText(
                    model,
                    {},
                    presentation =
                        RichPostPresentation(onLongPress = {
                            menu =
                                true
                        }),
                )
            }
        }
        rule.onNodeWithText("post text").performTouchInput { longClick(center) }
        rule.runOnIdle { assertTrue(menu) }
        rule
            .onNodeWithText(
                "post text",
            ).assert(SemanticsMatcher.keyNotDefined(SemanticsProperties.TextSelectionRange))
    }
}
