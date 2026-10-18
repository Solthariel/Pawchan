package com.solthariel.pawchan

import com.solthariel.pawchan.domain.model.PostId
import com.solthariel.pawchan.domain.model.PostLink
import com.solthariel.pawchan.thread.PostLinkTap
import com.solthariel.pawchan.thread.PostLinkTapAbilities
import com.solthariel.pawchan.thread.PostLinkTarget
import com.solthariel.pawchan.thread.postLinkTap
import org.junit.Assert.assertEquals
import org.junit.Test

class PostLinkTapTest {
    private val all = PostLinkTapAbilities(preview = true, thread = true, board = true, media = true)
    private val thread = defaultThreadAddress(100)

    @Test
    fun postLinkOpensPreview() {
        val target = PostLinkTarget(url = "https://2ch.su/b/res/100.html#105", thread = thread, postId = 105)

        assertEquals(
            PostLinkTap.Preview(PostLink.Reference(requireNotNull(PostId.parse(105)), thread)),
            postLinkTap(target, all),
        )
    }

    @Test
    fun postLinkWithoutPreviewOpensThreadAtPost() {
        val target = PostLinkTarget(url = "https://2ch.su/b/res/100.html#105", thread = thread, postId = 105)

        assertEquals(PostLinkTap.Thread(thread, 105), postLinkTap(target, all.copy(preview = false)))
    }

    @Test
    fun threadLinkOpensThread() {
        val target = PostLinkTarget(url = "https://2ch.su/b/res/100.html", thread = thread)

        assertEquals(PostLinkTap.Thread(thread, null), postLinkTap(target, all))
    }

    @Test
    fun boardLinkOpensCatalog() {
        val target = PostLinkTarget(url = "https://2ch.su/b/", board = defaultBoardAddress())

        assertEquals(PostLinkTap.Board(defaultBoardAddress()), postLinkTap(target, all))
    }

    @Test
    fun unknownOrUnsupportedLinkGoesToBrowser() {
        val board = PostLinkTarget(url = "https://2ch.su/b/", board = defaultBoardAddress())

        assertEquals(PostLinkTap.Browser, postLinkTap(null, all))
        assertEquals(PostLinkTap.Browser, postLinkTap(PostLinkTarget(url = "https://example.com"), all))
        assertEquals(PostLinkTap.Browser, postLinkTap(board, all.copy(board = false)))
    }
}
