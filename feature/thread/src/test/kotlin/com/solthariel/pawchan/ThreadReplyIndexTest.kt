package com.solthariel.pawchan

import com.solthariel.pawchan.domain.model.PostDocument
import com.solthariel.pawchan.domain.model.PostId
import com.solthariel.pawchan.domain.model.PostLink
import com.solthariel.pawchan.domain.model.PostTextRun
import com.solthariel.pawchan.thread.RichPostTextUi
import com.solthariel.pawchan.thread.ThreadPostUi
import com.solthariel.pawchan.thread.prepareThreadReplyIndex
import com.solthariel.pawchan.thread.withPostNavigation
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ThreadReplyIndexTest {
    @Test
    fun emptyThreadHasNoReplyGroups() {
        assertTrue(prepareThreadReplyIndex(emptyList()).isEmpty())
    }

    @Test
    fun repeatedReferencesCountTheReplyOnceAndKeepPostOrder() {
        val posts = listOf(toolsPost(1), referencePost(2, 1, 1), referencePost(3, 1))
        val group = requireNotNull(prepareThreadReplyIndex(posts)[1])

        assertEquals("2 ответа", group.label)
        assertEquals(listOf(2L, 3L), group.posts.map(ThreadPostUi::id))
    }

    @Test
    fun oneReplyCanAppearInSeveralGroups() {
        val post = referencePost(3, 1, 2)
        val index = prepareThreadReplyIndex(listOf(toolsPost(1), toolsPost(2), post))

        assertEquals(listOf(post), index[1]?.posts)
        assertEquals(listOf(post), index[2]?.posts)
        assertEquals("1 ответ", index[2]?.label)
    }

    @Test
    fun navigationRefreshRebuildsRepliesWithoutStalePosts() {
        val before = toolsContent(listOf(toolsPost(1), referencePost(2, 1)))
        val after = before.copy(posts = listOf(toolsPost(1), referencePost(3, 1))).withPostNavigation()

        assertEquals(
            listOf(3L),
            after.presentation.replyIndex[1]
                ?.posts
                ?.map(ThreadPostUi::id),
        )
        assertEquals("1 ответ", after.presentation.replyIndex[1]?.label)
    }

    private fun referencePost(
        id: Long,
        vararg targets: Long,
    ): ThreadPostUi {
        val document =
            PostDocument(
                targets.map { target ->
                    PostTextRun(">>$target ", link = PostLink.Reference(requireNotNull(PostId.parse(target))))
                },
            )
        return toolsPost(id).copy(document = document, richText = RichPostTextUi.create(document))
    }
}
