package com.solthariel.pawchan.data.network

import com.solthariel.pawchan.domain.model.PostLink
import com.solthariel.pawchan.domain.model.PostTextStyle
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PostDocumentParserTest {
    @Test
    fun parsesMakabaFormattingAndLinks() {
        val document =
            PostDocumentParser.parse(
                """
                <b>жирный</b><br>
                <span class="unkfunc">&gt;цитата</span>
                <a href="/b/res/42.html#43">&gt;&gt;43</a>
                <span class="spoiler">секрет</span>
                <span style="color: #ff0000">красный</span>
                <a href="https://2ch.su/b/">доска</a>
                """.trimIndent(),
            )

        assertEquals("жирный\n >цитата >>43 секрет красный доска", document.plainText.replace("\n\n", "\n"))
        assertTrue(document.runs.any { PostTextStyle.BOLD in it.styles })
        assertTrue(document.runs.any { PostTextStyle.GREENTEXT in it.styles })
        assertTrue(document.runs.any { PostTextStyle.RED in it.styles })
        assertTrue(document.runs.any { PostTextStyle.SPOILER in it.styles })
        val reference = document.runs.first { it.link is PostLink.Reference }.link as PostLink.Reference
        assertEquals(43L, reference.post.value)
        assertTrue(document.runs.any { it.link is PostLink.External })
    }

    @Test
    fun autoLinksUrlGluedToPreviousWordAndDecodesEntities() {
        val document = PostDocumentParser.parse("<p>Двачаt.me/datemeplz &gt;готово</p>")

        assertEquals("Двача t.me/datemeplz >готово\n", document.plainText)
        val linkRun = document.runs.first { it.link is PostLink.External }
        assertEquals("t.me/datemeplz", linkRun.text)
        assertEquals(
            "https://t.me/datemeplz",
            (linkRun.link as PostLink.External).url.value,
        )
    }
}
