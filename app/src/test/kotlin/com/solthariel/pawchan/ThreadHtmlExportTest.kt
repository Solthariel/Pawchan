package com.solthariel.pawchan

import androidx.compose.ui.text.AnnotatedString
import com.solthariel.pawchan.domain.model.PostDocument
import com.solthariel.pawchan.infrastructure.thread.export.escapeThreadHtml
import com.solthariel.pawchan.infrastructure.thread.export.threadHtml
import com.solthariel.pawchan.thread.ThreadExportDocument
import com.solthariel.pawchan.thread.ThreadExportPost
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ThreadHtmlExportTest {
    @Test fun exportedTextCannotInjectScript() {
        val html = threadHtml(exportFixture("<script>alert(1)</script> & текст"))
        assertTrue(html.contains("&lt;script&gt;"))
        assertFalse(html.contains("<script>"))
    }

    @Test fun htmlIncludesBothHeaderAndFullText() {
        val html = threadHtml(exportFixture("Полный текст", "Длинное имя"))
        assertTrue(html.contains("#1 Длинное имя"))
        assertTrue(html.contains("Полный текст"))
        assertEquals("&quot;&#39;&amp;&lt;&gt;", escapeThreadHtml("\"'&<>"))
    }

    @Test fun emptyThreadHtmlKeepsExistingFormatByteForByte() {
        val html = threadHtml(ThreadExportDocument(1, null, emptyList()))
        val expected =
            "<!doctype html><html lang=\"ru\"><meta charset=\"utf-8\"><title>Тред 1</title><style>" +
                "body{font-family:sans-serif;max-width:900px;margin:auto;padding:12px}" +
                "article{border-bottom:1px solid #aaa;padding:12px 0}" +
                ".head{font-size:12px;white-space:nowrap;overflow:auto}" +
                ".text{white-space:pre-wrap}img{max-width:72px;max-height:72px}</style><body></body></html>"
        assertEquals(expected, html)
    }

    private fun exportFixture(
        text: String,
        author: String = "Имя",
    ): ThreadExportDocument =
        ThreadExportDocument(
            1,
            "Тема",
            listOf(
                ThreadExportPost(
                    1,
                    AnnotatedString("#1 $author 05.10 09:01:11"),
                    null,
                    PostDocument.plain(text),
                    emptyList(),
                ),
            ),
        )
}
