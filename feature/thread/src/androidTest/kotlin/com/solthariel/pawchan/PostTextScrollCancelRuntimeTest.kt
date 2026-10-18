package com.solthariel.pawchan

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.unit.dp
import com.solthariel.pawchan.domain.model.PostDocument
import com.solthariel.pawchan.domain.model.PostId
import com.solthariel.pawchan.domain.model.PostLink
import com.solthariel.pawchan.domain.model.PostTextRun
import com.solthariel.pawchan.domain.settings.ThemePreference
import com.solthariel.pawchan.thread.RichPostText
import com.solthariel.pawchan.thread.RichPostTextUi
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class PostTextScrollCancelRuntimeTest {
    @get:Rule val compose = createComposeRule()

    @Test
    fun parentScrollTakesGestureWithoutOpeningReference() {
        var opened = 0
        lateinit var scroll: ScrollState
        val model =
            RichPostTextUi.create(
                PostDocument(listOf(PostTextRun(">>456", link = PostLink.Reference(requireNotNull(PostId.parse(456L)))))),
            )
        compose.setContent {
            ThreadTestTheme(ThemePreference.DARK) {
                scroll = rememberScrollState()
                Column(Modifier.height(180.dp).verticalScroll(scroll)) {
                    Spacer(Modifier.height(80.dp))
                    RichPostText(model, { opened++ })
                    Spacer(Modifier.height(1000.dp))
                }
            }
        }
        compose.onNodeWithText(">>456").performTouchInput {
            down(center)
            moveTo(center + Offset(0f, -40f))
            moveTo(center + Offset(0f, -200f))
            up()
        }
        compose.runOnIdle {
            assertEquals(0, opened)
            assertTrue("The text recognizer must not consume scrolling MOVE events", scroll.value > 0)
        }
    }
}
