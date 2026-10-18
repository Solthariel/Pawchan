package com.solthariel.pawchan

import androidx.compose.ui.text.AnnotatedString
import com.solthariel.pawchan.domain.model.PostDocument
import com.solthariel.pawchan.domain.model.PostTextRun
import com.solthariel.pawchan.domain.model.PostTextStyle
import com.solthariel.pawchan.thread.ThreadPostDisplay
import com.solthariel.pawchan.thread.exportDocument
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ThreadExportSnapshotTest {
    @Test fun htmlSnapshotKeepsAllPostsInPresentationOrder() {
        val content = toolsContent(listOf(toolsPost(9, "Первый"), toolsPost(3, "Второй")))
        val result = content.exportDocument()
        assertEquals(content.id, result.id)
        assertEquals(content.subject, result.subject)
        assertEquals(listOf("Первый", "Второй"), result.posts.map { it.document.plainText })
    }

    @Test fun shotSelectionUsesContentOrderNotSetOrder() {
        val result = toolsContent(listOf(toolsPost(9), toolsPost(3), toolsPost(7))).exportDocument(linkedSetOf(7, 9))
        assertEquals(listOf(9, 7), result.posts.map { it.index })
    }

    @Test fun emptyAndStaleSelectionNeverExportsAllPosts() {
        val content = toolsContent(listOf(toolsPost(9)))
        assertTrue(content.exportDocument(emptySet()).posts.isEmpty())
        assertTrue(content.exportDocument(setOf(100)).posts.isEmpty())
    }

    @Test fun preparedHeaderSubjectAndDocumentRunsArePreserved() {
        val original = toolsPost(4, "Text")
        val document = PostDocument(listOf(PostTextRun("Text", setOf(PostTextStyle.BOLD, PostTextStyle.SPOILER))))
        val display = ThreadPostDisplay(6, AnnotatedString("#4 Цветное имя"), subject = "Заголовок")
        val post = original.copy(document = document, metadata = original.metadata.copy(display = display))
        val result =
            toolsContent(listOf(post))
                .exportDocument()
                .posts
                .single()
        assertEquals(6, result.index)
        assertEquals(display.header, result.header)
        assertEquals(display.subject, result.subject)
        assertEquals(document, result.document)
    }
}
