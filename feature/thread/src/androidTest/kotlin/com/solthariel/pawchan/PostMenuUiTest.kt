package com.solthariel.pawchan

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextStyle
import com.solthariel.pawchan.thread.PostMenuCommand
import com.solthariel.pawchan.thread.PostMenuDialog
import com.solthariel.pawchan.thread.PostMenuOptions
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class PostMenuUiTest {
    @get:Rule val compose = createComposeRule()

    @Test
    fun absentLinkCapabilityDisablesOnlyTheLinkItem() {
        compose.setContent {
            com.solthariel.pawchan.thread.PostMenuContent(
                com.solthariel.pawchan.thread.PostMenuPage.COPY,
                PostMenuOptions(true, false, false, false, false, hasLink = false),
                {},
                {},
            )
        }
        compose.onNodeWithText("Ссылку").assertIsNotEnabled()
        compose.onNodeWithText("Текст").assertIsEnabled()
    }

    @Test
    fun copyAndHideSubmenusExposeAllCommandsAndBackReturnsToMain() {
        var command: PostMenuCommand? = null
        val options = PostMenuOptions(true, false, true, false, false, hasLink = true)
        compose.setContent { PostMenuDialog(options, { command = it }, {}) }
        compose.onNodeWithText("Копировать").performClick()
        compose.onNodeWithText("Текст").assertIsDisplayed()
        compose.onNodeWithText("Разметку").assertIsDisplayed()
        compose.onNodeWithText("Ссылку").assertIsDisplayed()
        compose.onNodeWithText("Назад").performClick()
        compose.onNodeWithText("Скрыть").performClick()
        compose.onNodeWithText("Дерево ответов").assertIsDisplayed()
        compose.onNodeWithText("Посты с тем же именем").assertIsDisplayed()
        compose.onNodeWithText("Похожие посты").assertIsDisplayed()
        compose.onNodeWithText("Этот пост").performClick()
        assertEquals(PostMenuCommand.HIDE, command)
    }

    @Test
    fun closedThreadBlocksReplyAndReportWithoutRepositoryIsDisabled() {
        compose.setContent { PostMenuDialog(PostMenuOptions(true, true, false, true, false, hasLink = true), {}, {}) }
        compose.onNodeWithText("Ответить").assertIsNotEnabled()
        compose.onNodeWithText("Цитировать").assertIsNotEnabled()
        compose.onNodeWithText("Пожаловаться").assertIsNotEnabled()
        compose.onNodeWithText("Моё сообщение").assertIsDisplayed()
        compose.onNodeWithText("Скрыть").assertIsDisplayed()
    }

    @Test
    fun addRuleFollowsOwnMessageAndDispatchesOnlyItsOwnCommand() {
        var command: PostMenuCommand? = null
        compose.setContent {
            PostMenuDialog(PostMenuOptions(true, false, false, false, false), { command = it }, {}, canAddRule = true)
        }
        val own = compose.onNodeWithText("Моё сообщение").fetchSemanticsNode().boundsInRoot
        val add = compose.onNodeWithText("Добавить правило").fetchSemanticsNode().boundsInRoot
        org.junit.Assert.assertTrue(add.top >= own.bottom)
        compose.onNodeWithText("Добавить правило").performClick()
        assertEquals(PostMenuCommand.ADD_FILTER, command)
    }

    @Test fun ownMessageKeepsBodyFontAndThemeAccentAndStillDispatchesOwn() {
        var command: PostMenuCommand? = null
        val accent = Color(0xFF6750A4)
        val ordinaryColor = Color(0xFF202020)
        compose.setContent {
            MaterialTheme(colorScheme = lightColorScheme(primary = accent, onSurface = ordinaryColor)) {
                PostMenuDialog(PostMenuOptions(true, false, true, false, false), { command = it }, {})
            }
        }
        val ordinary = textStyle("Ответить")
        val own = textStyle("Моё сообщение")
        assertEquals(ordinary.fontSize, own.fontSize)
        assertEquals(ordinary.fontWeight, own.fontWeight)
        assertEquals(ordinary.letterSpacing, own.letterSpacing)
        assertEquals(ordinaryColor, ordinary.color)
        assertEquals(accent, own.color)
        compose.onNodeWithText("Моё сообщение").performClick()
        assertEquals(PostMenuCommand.OWN, command)
    }

    @Test fun ownMessageUsesTheDarkThemeAccentToo() {
        val accent = Color(0xFFD0BCFF)
        compose.setContent {
            MaterialTheme(colorScheme = darkColorScheme(primary = accent)) {
                PostMenuDialog(PostMenuOptions(true, false, true, true, false), {}, {})
            }
        }
        assertEquals(accent, textStyle("Моё сообщение").color)
        assertEquals(textStyle("Ответить").fontSize, textStyle("Моё сообщение").fontSize)
    }

    private fun textStyle(label: String): TextStyle {
        val results = mutableListOf<TextLayoutResult>()
        compose.onNodeWithText(label).performSemanticsAction(SemanticsActions.GetTextLayoutResult) { it(results) }
        return results.single().layoutInput.style
    }
}
