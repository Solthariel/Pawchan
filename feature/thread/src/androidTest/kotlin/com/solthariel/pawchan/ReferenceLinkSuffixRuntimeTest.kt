package com.solthariel.pawchan

import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.click
import androidx.compose.ui.test.doubleClick
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextRange
import com.solthariel.pawchan.domain.model.BoardAddress
import com.solthariel.pawchan.domain.model.BoardId
import com.solthariel.pawchan.domain.model.PostDocument
import com.solthariel.pawchan.domain.model.PostId
import com.solthariel.pawchan.domain.model.PostLink
import com.solthariel.pawchan.domain.model.PostTextRun
import com.solthariel.pawchan.domain.model.SiteId
import com.solthariel.pawchan.domain.model.ThreadAddress
import com.solthariel.pawchan.domain.model.ThreadId
import com.solthariel.pawchan.domain.settings.ThemePreference
import com.solthariel.pawchan.thread.RichPostPresentation
import com.solthariel.pawchan.thread.RichPostText
import com.solthariel.pawchan.thread.RichPostTextUi
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class ReferenceLinkSuffixRuntimeTest {
    @get:Rule val compose = createComposeRule()

    @Test fun tappingVisualOpSuffixStillOpensTheOriginalReference() {
        var tapped = 0L
        var layout: TextLayoutResult? = null
        val model = model()
        compose.setContent {
            ThreadTestTheme(ThemePreference.SYSTEM) {
                RichPostText(
                    model,
                    { tapped = it },
                    Modifier.testTag("post"),
                    RichPostPresentation(onTextLayout = { layout = it }),
                )
            }
        }
        compose.onNodeWithTag("post").performTouchInput {
            click(requireNotNull(layout).getBoundingBox(model.initialText.text.indexOf("(OP)")).center)
        }
        compose.runOnIdle { assertEquals(252013L, tapped) }
    }

    @Test fun selectionLayoutRetainsOriginalTextAndCharacterCount() {
        val model = model()
        var layout: TextLayoutResult? = null
        compose.setContent {
            ThreadTestTheme(ThemePreference.SYSTEM) {
                RichPostText(model, {}, Modifier.testTag("post"), RichPostPresentation(onTextLayout = { layout = it }))
            }
        }
        compose
            .onNodeWithTag(
                "post",
            ).performTouchInput {
                doubleClick(requireNotNull(layout).getBoundingBox(model.initialText.length - 1).center)
            }
        compose.onNodeWithTag("post").assert(
            SemanticsMatcher.expectValue(
                SemanticsProperties.TextSelectionRange,
                TextRange(model.initialText.text.indexOf("body"), model.initialText.length),
            ),
        )
        compose.runOnIdle { assertEquals(model.initialText.text, requireNotNull(layout).layoutInput.text.text) }
    }

    private fun model(): RichPostTextUi {
        val board = BoardAddress(requireNotNull(SiteId.parse("dvach")), requireNotNull(BoardId.parse("test")))
        val address = ThreadAddress(board, requireNotNull(ThreadId.parse(252013)))
        val link = PostLink.Reference(requireNotNull(PostId.parse(252013)), address)
        val runs = listOf(PostTextRun(">>252013 (OP)", link = link), PostTextRun("\nbody"))
        return RichPostTextUi.create(PostDocument(runs))
    }
}
