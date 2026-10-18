package com.solthariel.pawchan

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.click
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.longClick
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performTouchInput
import com.solthariel.pawchan.domain.model.PostDocument
import com.solthariel.pawchan.domain.model.PostId
import com.solthariel.pawchan.domain.model.PostLink
import com.solthariel.pawchan.domain.model.PostTextRun
import com.solthariel.pawchan.domain.settings.ThemePreference
import com.solthariel.pawchan.thread.LocalPostLinkAddress
import com.solthariel.pawchan.thread.RichPostText
import com.solthariel.pawchan.thread.RichPostTextUi
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class PostLinkMenuRuntimeTest {
    @get:Rule val compose = createComposeRule()

    @Test
    fun holdingReferenceShowsCopyWithoutShareAndWithoutOpeningPreview() {
        val model =
            RichPostTextUi.create(
                PostDocument(listOf(PostTextRun(">>456", link = PostLink.Reference(requireNotNull(PostId.parse(456L)))))),
            )
        var opened = 0
        compose.setContent {
            ThreadTestTheme(ThemePreference.DARK) {
                ThreadTestLinkHost {
                    CompositionLocalProvider(LocalPostLinkAddress provides defaultThreadAddress(123L)) {
                        RichPostText(model, { opened++ })
                    }
                }
            }
        }
        compose.onNodeWithText(">>456").performTouchInput { longClick(center) }
        compose.onNodeWithText("Копировать ссылку").assertExists()
        compose.onNodeWithText("Отправить ссылку").assertDoesNotExist()
        compose.runOnIdle { assertEquals(0, opened) }
        val menu = compose.onNodeWithTag("post-link-menu").fetchSemanticsNode().boundsInRoot
        val row = compose.onNodeWithText("Копировать ссылку").fetchSemanticsNode().boundsInRoot
        compose.runOnIdle {
            assertTrue("Menu must wrap two rows without an empty footer", menu.height <= row.height * 3)
            assertEquals(menu.width, row.width, 1f)
        }
        compose.onNodeWithText("Копировать ссылку").performTouchInput {
            click(Offset(width - 2f, center.y))
        }
        compose.onNodeWithText("Копировать ссылку").assertDoesNotExist()
    }
}
