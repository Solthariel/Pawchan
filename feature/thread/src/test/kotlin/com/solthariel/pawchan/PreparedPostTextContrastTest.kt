package com.solthariel.pawchan

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import com.solthariel.pawchan.domain.model.PostDocument
import com.solthariel.pawchan.domain.model.PostId
import com.solthariel.pawchan.domain.model.PostLink
import com.solthariel.pawchan.domain.model.PostTextRun
import com.solthariel.pawchan.domain.model.PostTextStyle
import com.solthariel.pawchan.thread.RichPostTextUi
import org.junit.Assert.assertTrue
import org.junit.Test

class PreparedPostTextContrastTest {
    @Test fun preparedSemanticTextHasNormalTextContrastOnLightSurface() {
        val reference = PostLink.Reference(requireNotNull(PostId.parse(42)))
        val document =
            PostDocument(
                listOf(
                    PostTextRun(">>42", link = reference),
                    PostTextRun(">quote", styles = setOf(PostTextStyle.GREENTEXT)),
                    PostTextRun("red", styles = setOf(PostTextStyle.RED)),
                ),
            )
        val prepared = RichPostTextUi.create(document).initialText
        prepared.spanStyles.forEach { span ->
            val ratio = (Color.White.luminance() + LUMINANCE_OFFSET) / (span.item.color.luminance() + LUMINANCE_OFFSET)
            assertTrue("Prepared post text contrast=$ratio", ratio >= MIN_TEXT_CONTRAST)
        }
    }
}

private const val LUMINANCE_OFFSET = 0.05f
private const val MIN_TEXT_CONTRAST = 4.5f
