package com.solthariel.pawchan

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.text.TextLayoutResult
import com.solthariel.pawchan.core.ui.DARK_POST_TEXT_COLORS
import com.solthariel.pawchan.core.ui.LIGHT_POST_TEXT_COLORS
import com.solthariel.pawchan.domain.model.PostDocument
import com.solthariel.pawchan.domain.model.PostTextRun
import com.solthariel.pawchan.domain.model.PostTextStyle
import com.solthariel.pawchan.thread.RichPostPresentation
import com.solthariel.pawchan.thread.RichPostText
import com.solthariel.pawchan.thread.RichPostTextUi
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class PostTextThemeRuntimeTest {
    @get:Rule val compose = createComposeRule()

    @Test fun existingPostChangesSemanticColorWhenThemeChangesWithoutReloadingModel() {
        val dark = mutableStateOf(false)
        val document = PostDocument(listOf(PostTextRun(">quote", styles = setOf(PostTextStyle.GREENTEXT))))
        val model = RichPostTextUi.create(document)
        var layout: TextLayoutResult? = null
        compose.setContent {
            MaterialTheme(colorScheme = if (dark.value) darkColorScheme() else lightColorScheme()) {
                RichPostText(model, {}, presentation = RichPostPresentation(onTextLayout = { layout = it }))
            }
        }
        compose.runOnIdle {
            assertEquals(
                LIGHT_POST_TEXT_COLORS.quote,
                requireNotNull(layout)
                    .layoutInput.text.spanStyles
                    .first()
                    .item.color,
            )
            dark.value = true
        }
        compose.runOnIdle {
            assertEquals(
                DARK_POST_TEXT_COLORS.quote,
                requireNotNull(layout)
                    .layoutInput.text.spanStyles
                    .first()
                    .item.color,
            )
            assertEquals(document.plainText, requireNotNull(layout).layoutInput.text.text)
        }
    }
}
