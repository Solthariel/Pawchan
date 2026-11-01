package com.solthariel.pawchan

import com.solthariel.pawchan.core.ui.DARK_POST_TEXT_COLORS
import com.solthariel.pawchan.core.ui.LIGHT_POST_TEXT_COLORS
import com.solthariel.pawchan.domain.model.PostDocument
import com.solthariel.pawchan.domain.model.PostId
import com.solthariel.pawchan.domain.model.PostLink
import com.solthariel.pawchan.domain.model.PostTextRun
import com.solthariel.pawchan.domain.model.PostTextStyle
import com.solthariel.pawchan.thread.REFERENCE_TAG
import com.solthariel.pawchan.thread.RichPostTextUi
import com.solthariel.pawchan.thread.SPOILER_TAG
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Test

class PostTextThemeColorsTest {
    @Test fun ordinaryTextReusesOnePreparedStringForBothThemes() {
        val model = RichPostTextUi.create(PostDocument.plain("Обычный текст"))
        assertSame(model.text(LIGHT_POST_TEXT_COLORS), model.text(DARK_POST_TEXT_COLORS))
    }

    @Test fun themeVariantsKeepTextAndAnnotationsButAdaptSemanticColors() {
        val reference = PostLink.Reference(requireNotNull(PostId.parse(42)))
        val document =
            PostDocument(
                listOf(
                    PostTextRun(">>42", link = reference),
                    PostTextRun(">quote", styles = setOf(PostTextStyle.GREENTEXT)),
                    PostTextRun("red", styles = setOf(PostTextStyle.RED)),
                ),
            )
        val model = RichPostTextUi.create(document)
        val light = model.text(LIGHT_POST_TEXT_COLORS)
        val dark = model.text(DARK_POST_TEXT_COLORS)
        assertEquals(document.plainText, light.text)
        assertEquals(light.text, dark.text)
        assertEquals(
            light.getStringAnnotations(REFERENCE_TAG, 0, light.length),
            dark.getStringAnnotations(REFERENCE_TAG, 0, dark.length),
        )
        listOf(LIGHT_POST_TEXT_COLORS, DARK_POST_TEXT_COLORS).forEach { colors ->
            assertEquals(
                listOf(colors.link, colors.quote, colors.red),
                model.text(colors).spanStyles.map { it.item.color },
            )
            assertSame(model.text(colors), model.text(colors))
        }
    }

    @Test fun revealingSpoilerUsesActiveThemeWithoutChangingTheSourceText() {
        val document =
            PostDocument(
                listOf(PostTextRun(">hidden", styles = setOf(PostTextStyle.SPOILER, PostTextStyle.GREENTEXT))),
            )
        val model = RichPostTextUi.create(document)
        listOf(LIGHT_POST_TEXT_COLORS, DARK_POST_TEXT_COLORS).forEach { colors ->
            val hidden = model.text(colors)
            val revealed = model.reveal(setOf(0), colors)
            assertEquals(hidden.text, revealed.text)
            assertEquals(
                colors.quote,
                revealed.spanStyles
                    .single()
                    .item.color,
            )
            assertEquals(1, hidden.getStringAnnotations(SPOILER_TAG, 0, hidden.length).size)
            assertEquals(0, revealed.getStringAnnotations(SPOILER_TAG, 0, revealed.length).size)
        }
    }
}
