package com.solthariel.pawchan

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.performClick
import com.solthariel.pawchan.composer.ComposerDraftState
import com.solthariel.pawchan.composer.ComposerPostingOptions
import com.solthariel.pawchan.domain.posting.PostingRules
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class ComposerPostingOptionsUiTest {
    @get:Rule val compose = createComposeRule()

    @Test fun authorIconTogglesBothWaysWithoutChangingOtherOptions() {
        val draft = mutableStateOf(ComposerDraftState())
        compose.setContent { MaterialTheme { ComposerPostingOptions(draft.value, null, true) { draft.value = it } } }
        val author = compose.onNodeWithContentDescription("Создатель треда")
        author.assertIsOff().performClick().assertIsOn()
        compose.runOnIdle {
            assertTrue(draft.value.options.threadAuthor)
            assertFalse(draft.value.options.sage)
        }
        author.performClick().assertIsOff()
    }

    @Test fun unavailableSageIsNotShownAndDisabledOptionsCannotToggle() {
        compose.setContent { MaterialTheme { ComposerPostingOptions(ComposerDraftState(), null, false) {} } }
        compose.onNodeWithContentDescription("Не поднимать").assertDoesNotExist()
        compose.onNodeWithContentDescription("Создатель треда").assertIsNotEnabled()
    }

    @Test fun sageAndAuthorCanBeEnabledIndependentlyTogether() {
        val draft = mutableStateOf(ComposerDraftState())
        compose.setContent { MaterialTheme { ComposerPostingOptions(draft.value, PostingRules(), true) { draft.value = it } } }
        val sage = compose.onNodeWithContentDescription("Не поднимать")
        val author = compose.onNodeWithContentDescription("Создатель треда")
        sage.performClick().assertIsOn()
        author.assertIsOff().performClick().assertIsOn()
        sage.performClick().assertIsOff()
        compose.runOnIdle {
            assertTrue(draft.value.options.threadAuthor)
            assertFalse(draft.value.options.sage)
        }
    }
}
