package com.solthariel.pawchan.data.network

import com.solthariel.pawchan.domain.archive.ArchiveSubmission
import com.solthariel.pawchan.domain.model.BoardAddress
import com.solthariel.pawchan.domain.model.BoardId
import com.solthariel.pawchan.domain.model.SiteId
import com.solthariel.pawchan.domain.model.ThreadAddress
import com.solthariel.pawchan.domain.model.ThreadId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** Synthetic contract fixtures, not evidence of current live-site markup compatibility. */
class ArhivachDecoderTest {
    private val address =
        ThreadAddress(
            BoardAddress(requireNotNull(SiteId.parse("dvach")), requireNotNull(BoardId.parse("b"))),
            requireNotNull(ThreadId.parse(100)),
        )
    private val url = "https://arhivach.vc/thread/55/"
    private val html = """<a href="https://2ch.su/b/res/100.html">Источник</a>
        <div class="post" id="post_100"><div class="post_comment">Hello &gt;&gt;101</div></div>
        <div class="post" id="post_101"><div class="post_comment">Reply</div></div>"""

    @Test fun acceptsOnlyHttpsCanonicalThreadUrls() {
        assertTrue(validArhivachUrl(url))
        listOf(
            "http://arhivach.vc/thread/55/",
            "https://arhivach.vc.evil.org/thread/55/",
            "https://arhivach.vc:444/thread/55/",
            "https://user@arhivach.vc/thread/55/",
            "https://arhivach.vc/thread/55/?next=evil",
        ).forEach {
            assertFalse(it, validArhivachUrl(it))
        }
    }

    @Test fun unknownMarkupOrWrongSourceNeverBecomesEmptySuccessfulThread() {
        assertNull(decodeArhivachThread(address, url, "<p>challenge</p>"))
        assertNull(decodeArhivachThread(address, url, html.replace("/b/res/100", "/a/res/100")))
        assertNull(decodeArhivachThread(address, url, html.replace("post_100", "post_99")))
    }

    @Test fun readsLiveMarkupHeadAndBodyWithoutSourceLink() {
        val live =
            """<div class="post" id="post100" postid="100"><div class="post_head">
            <h1 class="post_subject">Тема</h1> <span class="poster_name">Аноним</span>
            <span class="post_time">10/10/26 Суб 20:52:04</span> <span class="post_num">#1</span></div>
            <div class="post_comment"><div class="post_image_block"></div><div class="post_comment_body">
            Привет</div></div></div>
            <div class="post" id="post101" postid="101"><div class="post_head">
            <img alt="sage" class="poster_sage"/><span class="poster_name">Аноним</span>
            <span class="post_time">10/10/26 Суб 20:55:37</span></div>
            <div class="post_comment"><div class="post_comment_body">
            Ответ</div></div></div>"""
        val snapshot = requireNotNull(decodeArhivachThread(address, url, live))
        val (op, reply) = snapshot.posts
        assertEquals("Привет", op.content.document.plainText)
        assertEquals("Тема", op.content.metadata.subject)
        assertEquals("10/10/26 Суб 20:52:04", op.content.metadata.date)
        assertEquals(1_791_654_724L, op.content.details.timestamp)
        assertTrue(reply.content.metadata.sage)
        assertEquals("Аноним", reply.content.metadata.name)
    }

    @Test fun requiresUniqueOriginalPostIdsAndKeepsNativeDocument() {
        val snapshot = requireNotNull(decodeArhivachThread(address, url, html))
        assertEquals(listOf(100L, 101L), snapshot.posts.map { it.address.post.value })
        assertTrue(snapshot.flags.closed)
        assertTrue(snapshot.flags.archived)
        assertEquals(
            "Reply",
            snapshot.posts
                .last()
                .content.document.plainText,
        )
        assertNull(decodeArhivachThread(address, url, html.replace("post_101", "post_100")))
    }

    @Test fun formSubmissionRecognizesRedirectLinkCaptchaAndErrors() {
        assertEquals(
            ArchiveSubmission.Accepted("https://arhivach.vc/thread/123/"),
            decodeFormSubmission(302, "https://arhivach.vc/thread/123/", ""),
        )
        assertEquals(
            ArchiveSubmission.Accepted("https://arhivach.vc/thread/77/"),
            decodeFormSubmission(
                200,
                null,
                """<div class="alert alert-success">Добавлен <a href="/thread/77/">тред</a></div>""",
            ),
        )
        assertEquals(
            ArchiveSubmission.Accepted("https://arhivach.vc/thread/1441109/", alreadyArchived = true),
            decodeFormSubmission(
                200,
                null,
                """<a href="/thread/5/">Случайный</a><div class="alert"><h4>Тред уже был добавлен ранее.</h4>""" +
                    """<div class="thread_info_block"><a class="thread_title" title="Перейти к треду" """ +
                    """href="https://arhivach.vc/thread/1441109/"><b>Тред</b></a></div></div>""",
            ),
        )
        assertEquals(
            ArchiveSubmission.WrongCaptcha("Неверная капча"),
            decodeFormSubmission(
                200,
                null,
                """<div class="alert alert-error"><button class="close">x</button>Ошибка ввода капчи.</div>""",
            ),
        )
        assertEquals(
            ArchiveSubmission.Rejected("Тред не найден"),
            decodeFormSubmission(200, null, """<div class="alert alert-error">Тред <b>не</b> найден</div>"""),
        )
        assertTrue(decodeFormSubmission(200, null, "<html>blocked</html>") is ArchiveSubmission.Rejected)
    }
}
