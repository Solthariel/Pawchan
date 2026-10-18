package com.solthariel.pawchan

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.UriHandler
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.longClick
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.text.TextLayoutResult
import com.solthariel.pawchan.domain.model.PostDocument
import com.solthariel.pawchan.domain.model.PostId
import com.solthariel.pawchan.domain.model.PostLink
import com.solthariel.pawchan.domain.model.PostTextRun
import com.solthariel.pawchan.domain.model.PostTextStyle
import com.solthariel.pawchan.domain.model.RemoteUrl
import com.solthariel.pawchan.domain.settings.ThemePreference
import com.solthariel.pawchan.thread.LocalPostLinkAddress
import com.solthariel.pawchan.thread.RichPostPresentation
import com.solthariel.pawchan.thread.RichPostText
import com.solthariel.pawchan.thread.RichPostTextUi
import com.solthariel.pawchan.thread.SPOILER_TAG
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class PostTextImmediateTapRuntimeTest {
    @get:Rule val compose = createComposeRule()

    @Test
    fun referenceOpensBeforeDoubleTapTimeout() {
        var opened = 0
        showReference { opened++ }
        compose.mainClock.autoAdvance = false
        compose.onNodeWithTag("reference").performTouchInput {
            down(center)
            advanceEventTime(40)
            up()
        }
        compose.mainClock.advanceTimeByFrame()
        compose.runOnIdle { assertEquals(1, opened) }
    }

    @Test
    fun holdingReferenceShowsMenuAndReleaseDoesNotOpenPost() {
        var opened = 0
        showReference { opened++ }
        compose.onNodeWithTag("reference").performTouchInput { longClick(center) }
        compose.onNodeWithText("Копировать ссылку").assertExists()
        compose.runOnIdle { assertEquals(0, opened) }
    }

    @Test
    fun leavingReferenceCancelsAndDoesNotRearmWhenFingerReturns() {
        var opened = 0
        showReference { opened++ }
        compose.onNodeWithTag("reference").performTouchInput {
            down(center)
            moveTo(Offset(width + 200f, center.y))
            moveTo(center)
            up()
        }
        compose.runOnIdle { assertEquals(0, opened) }
        compose.onNodeWithText("Копировать ссылку").assertDoesNotExist()
    }

    @Test
    fun secondPointerCancelsLinkOpening() {
        var opened = 0
        showReference { opened++ }
        compose.onNodeWithTag("reference").performTouchInput {
            down(0, center)
            down(1, center + Offset(10f, 0f))
            up(1)
            up(0)
        }
        compose.runOnIdle { assertEquals(0, opened) }
    }

    @Test
    fun hiddenSpoilerRevealsImmediatelyWithoutFollowingUnderlyingLink() {
        var opened = 0
        var layout: TextLayoutResult? = null
        val model = referenceModel(setOf(PostTextStyle.SPOILER))
        compose.setContent {
            ThreadTestTheme(ThemePreference.DARK) {
                RichPostText(model, { opened++ }, presentation = RichPostPresentation(onTextLayout = { layout = it }))
            }
        }
        compose.mainClock.autoAdvance = false
        compose.onNodeWithText(">>456").performTouchInput {
            down(center)
            advanceEventTime(40)
            up()
        }
        compose.mainClock.advanceTimeByFrame()
        compose.runOnIdle {
            assertEquals(0, opened)
            val text = requireNotNull(layout).layoutInput.text
            assertTrue(text.getStringAnnotations(SPOILER_TAG, 0, text.length).isEmpty())
        }
    }

    @Test
    fun externalUrlDispatchesBeforeDoubleTapTimeout() {
        var opened: String? = null
        val uri =
            object : UriHandler {
                override fun openUri(uri: String) {
                    opened = uri
                }
            }
        val url = "https://example.test/"
        val model =
            RichPostTextUi.create(
                PostDocument(listOf(PostTextRun(url, link = PostLink.External(requireNotNull(RemoteUrl.parse(url)))))),
            )
        compose.setContent {
            ThreadTestTheme(ThemePreference.DARK) {
                CompositionLocalProvider(LocalUriHandler provides uri) {
                    RichPostText(model, {})
                }
            }
        }
        compose.mainClock.autoAdvance = false
        compose.onNodeWithText(url).performTouchInput {
            down(center)
            advanceEventTime(40)
            up()
        }
        compose.mainClock.advanceTimeByFrame()
        compose.runOnIdle { assertEquals(url, opened) }
    }

    private fun showReference(onOpen: () -> Unit) {
        val model = referenceModel(emptySet())
        compose.setContent {
            ThreadTestTheme(ThemePreference.DARK) {
                ThreadTestLinkHost {
                    CompositionLocalProvider(LocalPostLinkAddress provides defaultThreadAddress(123L)) {
                        RichPostText(model, { onOpen() }, Modifier.testTag("reference"))
                    }
                }
            }
        }
    }

    private fun referenceModel(styles: Set<PostTextStyle>): RichPostTextUi =
        RichPostTextUi.create(
            PostDocument(
                listOf(PostTextRun(">>456", styles, PostLink.Reference(requireNotNull(PostId.parse(456L))))),
            ),
        )
}
