package com.solthariel.pawchan

import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.solthariel.pawchan.composer.ComposerDraftState
import com.solthariel.pawchan.composer.ComposerIconSelector
import com.solthariel.pawchan.domain.posting.PostingIcon
import com.solthariel.pawchan.domain.posting.PostingIdentityRules
import com.solthariel.pawchan.domain.posting.PostingRules
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class Fix12ComposerUiTest {
    @get:Rule val compose = createComposeRule()

    @Test
    fun choosingAndClearingBoardIconPreservesOtherFields() {
        val draft = mutableStateOf(ComposerDraftState(email = "test@example.test"))
        compose.setContent {
            Column {
                ComposerIconSelector(
                    draft.value,
                    PostingRules(
                        identity =
                            PostingIdentityRules(
                                icons = listOf(PostingIcon(7, "Рено"), PostingIcon(1, "Альфа Ромео")),
                            ),
                    ),
                    true,
                    onChange = { draft.value = it },
                )
            }
        }
        compose.onNodeWithText("Нет иконки").performClick()
        compose.onNodeWithText("Рено").performClick()
        assertEquals(7, draft.value.options.icon)
        assertEquals("test@example.test", draft.value.email)
        compose.onNodeWithText("Рено").performClick()
        compose.onNodeWithText("Нет иконки").performClick()
        assertEquals(null, draft.value.options.icon)
    }
}
