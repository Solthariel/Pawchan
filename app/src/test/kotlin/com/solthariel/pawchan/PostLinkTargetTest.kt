package com.solthariel.pawchan

import com.solthariel.pawchan.domain.model.PostId
import com.solthariel.pawchan.domain.model.PostLink
import com.solthariel.pawchan.domain.model.RemoteUrl
import com.solthariel.pawchan.infrastructure.sites.DvachWebLinks
import com.solthariel.pawchan.infrastructure.sites.ForumWebLink
import com.solthariel.pawchan.infrastructure.thread.AppPostLinkResolver
import com.solthariel.pawchan.navigation.defaultBoardAddress
import com.solthariel.pawchan.navigation.defaultThreadAddress
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PostLinkTargetTest {
    private val resolver = AppPostLinkResolver(listOf(DvachWebLinks()))

    @Test
    fun localReferenceCopiesAbsolutePostLinkNotQuoteText() {
        val target =
            resolver.resolve(
                PostLink.Reference(requireNotNull(PostId.parse(456L))),
                defaultThreadAddress(123L),
            )
        assertEquals("https://2ch.su/b/res/123.html#456", target?.url)
        assertEquals(defaultThreadAddress(123L), target?.thread)
        assertNull(target?.attachment)
    }

    @Test
    fun crossThreadReferenceDoesNotUseCurrentThread() {
        val target =
            resolver.resolve(
                PostLink.Reference(requireNotNull(PostId.parse(457L)), defaultThreadAddress(999L)),
                defaultThreadAddress(123L),
            )
        assertEquals("https://2ch.su/b/res/999.html#457", target?.url)
        assertNull(resolver.resolve(PostLink.Reference(requireNotNull(PostId.parse(456L))), null))
    }

    @Test
    fun threadAndFileActionsAreClassifiedIndependently() {
        val thread = target("https://2ch.hk/test/res/789.html#800")
        assertEquals(789L, thread?.thread?.thread?.value)
        assertEquals(
            "test",
            thread
                ?.thread
                ?.board
                ?.board
                ?.value,
        )
        assertNull(thread?.attachment)
        val file = target("https://cdn.example.test/files/picture.PNG?token=123")
        assertEquals("picture.PNG", file?.attachment?.fileName)
        assertEquals("https://cdn.example.test/files/picture.PNG?token=123", file?.attachment?.originalUrl)
        assertNull(file?.thread)
        assertTrue(target("https://example.test/file.mp4")?.attachment != null)
    }

    @Test
    fun lookalikeHostsAndWebPagesAreNotDownloadOrThreadTargets() {
        assertNull(target("https://2ch.hk.evil.test/b/res/123.html")?.thread)
        assertNull(target("https://example.test/page?name=video.mp4")?.attachment)
        assertNull(target("https://2ch.hk/b/res/0.html")?.thread)
        assertNull(target("https://user:password@2ch.hk/b/res/123.html"))
        assertNull(target("https://invalid host/file.png"))
    }

    @Test
    fun mirrorHostsPostAnchorsBoardsAndSiteMediaAreRecognized() {
        val post = target("https://2ch.su/b/res/123.html#post456")
        assertEquals(defaultThreadAddress(123L), post?.thread)
        assertEquals(456L, post?.postId)
        assertEquals(defaultThreadAddress(123L), target("https://2ch.org/b/res/123.html")?.thread)
        assertEquals(defaultBoardAddress(), target("https://2ch.life/b/")?.board)
        assertEquals(defaultBoardAddress(), target("https://2ch.hk/b/2.html")?.board)
        assertTrue(target("https://2ch.su/b/src/123/17000.webm")?.viewable != null)
        // Прямая ссылка на файл знает тред, но не пост: «Открыть пост» ищет пост по файлу.
        assertEquals(defaultThreadAddress(123L), target("https://2ch.su/b/src/123/17000.webm")?.fileThread)
        assertNull(target("https://2ch.su/b/src/123/17000.webm")?.thread)
    }

    @Test
    fun addressFieldUnderstandsShortForms() {
        val board = defaultBoardAddress()
        assertEquals(ForumWebLink.Thread(defaultThreadAddress(123L), null), resolver.addressInput("b 123", null))
        assertEquals(ForumWebLink.Thread(defaultThreadAddress(123L), null), resolver.addressInput("/b/123", null))
        assertEquals(ForumWebLink.Board(board), resolver.addressInput("/b/", null))
        assertEquals(ForumWebLink.Board(board), resolver.addressInput("b", null))
        assertEquals(ForumWebLink.Thread(defaultThreadAddress(123L), null), resolver.addressInput("123", board))
        assertEquals(
            ForumWebLink.Thread(defaultThreadAddress(123L), 456L),
            resolver.addressInput("https://2ch.su/b/res/123.html#456", null),
        )
        assertNull(resolver.addressInput("123", null))
    }

    private fun target(url: String) = resolver.resolve(PostLink.External(requireNotNull(RemoteUrl.parse(url))), null)
}
