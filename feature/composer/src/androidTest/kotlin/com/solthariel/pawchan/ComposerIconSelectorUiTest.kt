package com.solthariel.pawchan

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.solthariel.pawchan.composer.ComposerDraftState
import com.solthariel.pawchan.composer.ComposerIconSelector
import com.solthariel.pawchan.domain.posting.PostingIcon
import com.solthariel.pawchan.domain.posting.PostingIdentityRules
import com.solthariel.pawchan.domain.posting.PostingRules
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test

class ComposerIconSelectorUiTest {
    @get:Rule val compose = createComposeRule()

    private val rules =
        PostingRules(
            identity = PostingIdentityRules(icons = listOf(PostingIcon(1, "Альфа Ромео"), PostingIcon(2, "Рено"))),
        )

    @Test fun selectionAndClearKeepLabelsAndOtherDraftFieldsUnchanged() {
        val draft = mutableStateOf(ComposerDraftState(comment = "Текст сообщения"))
        compose.setContent {
            MaterialTheme {
                ComposerIconSelector(draft.value, rules, true, { draft.value = it }, compact = true)
            }
        }
        compose.onNodeWithContentDescription("Выбрать иконку поста").performClick()
        compose.onNodeWithText("Нет иконки").assertIsSelected()
        compose.onNodeWithText("Альфа Ромео").assertIsNotSelected().performClick()
        compose.runOnIdle {
            assertEquals(1, draft.value.options.icon)
            assertEquals("Текст сообщения", draft.value.comment)
        }
        compose.onNodeWithContentDescription("Иконка поста: Альфа Ромео").performClick()
        compose.onNodeWithText("Альфа Ромео").assertIsSelected()
        compose.onNodeWithText("✓ Альфа Ромео").assertDoesNotExist()
        compose.onNodeWithText("Нет иконки").assertIsNotSelected()
        compose.onNodeWithText("Рено").assertIsNotSelected().performClick()
        compose.runOnIdle { assertEquals(2, draft.value.options.icon) }
        compose.onNodeWithContentDescription("Иконка поста: Рено").performClick()
        compose.onNodeWithText("Рено").assertIsSelected()
        compose.onNodeWithText("Нет иконки").performClick()
        compose.runOnIdle { assertNull(draft.value.options.icon) }
        compose.onNodeWithContentDescription("Выбрать иконку поста").performClick()
        compose.onNodeWithText("Нет иконки").assertIsSelected()
        compose.onNodeWithText("✓ Нет иконки").assertDoesNotExist()
    }

    @Test fun disabledSelectorCannotOpenMenu() {
        compose.setContent {
            MaterialTheme {
                ComposerIconSelector(ComposerDraftState(), rules, false, {}, compact = true)
            }
        }
        compose.onNodeWithContentDescription("Выбрать иконку поста").assertIsNotEnabled()
        compose.onNodeWithText("Нет иконки").assertDoesNotExist()
    }
}
