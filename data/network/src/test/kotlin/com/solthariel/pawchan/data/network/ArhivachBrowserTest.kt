package com.solthariel.pawchan.data.network

import com.solthariel.pawchan.domain.model.DomainFailure
import com.solthariel.pawchan.domain.model.DomainOutcome
import com.solthariel.pawchan.domain.model.PostLink
import com.solthariel.pawchan.domain.model.PostTextStyle
import kotlinx.coroutines.runBlocking
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ArhivachBrowserTest {
    @Test fun listPreservesWebsiteStatusDatePreviewFavoriteAndGroupedMedia() {
        val html = """<div class="thread_list"><table><tr id="thread_row_55">
            <td class="thread_control"><span class="label label-warning">Активный</span>
            <span class="thread_posts_count">23</span>
            <a id="infav55" style="" title="Удалить из закладок"></a></td>
            <td class="thread_desc">
            <div class="thread_images">
            <div class="post_image_block"><a class="expand_image"
            onclick="expand_image2('55_1','/storage/f/a.jpg',event); return false;"><img src="/storage/t/a.jpg"></a></div>
            <div class="post_image_block"><a class="expand_image"
            onclick="expand_image2('55_2','/storage/f/b.jpg',event); return false;"><img src="/storage/t/b.jpg"></a></div>
            <div class="post_image_block"><a class="expand_image"
            onclick="expand_image2('55_3','/storage/f/c.jpg',event); return false;"><img src="/storage/t/c.jpg"></a></div>
            <div class="post_image_block"><a class="expand_image"
            onclick="expand_image2('55_4','/storage/f/d.jpg',event); return false;"><img src="/storage/t/d.jpg"></a></div>
            </div>
            <div class="thread_text"><a href="/thread/55/"><b>Тема</b> — Текст-превью</a></div>
            <div class="thread_tags"><a href="/index/?tags=2">/b/ - Бред</a></div>
            </td><td class="thread_date"><span title="Обновлен: 2026-11-14 04:30:00
            Посл.сообщение: 2026-11-14 04:29:57">2026-11-14 04:29:57</span></td>
            </tr></table></div>"""
        val summary =
            requireNotNull(decodeArchiveThreadList(html, "https://arhivach.vc/index/", 25))
                .threads
                .single()

        assertEquals("Тема", summary.title)
        assertEquals("Текст-превью", summary.metadata.previewText)
        assertEquals("Активный", summary.metadata.status)
        assertEquals("2026-11-14 04:29:57", summary.metadata.date)
        assertEquals("2026-11-14 04:30:00", summary.metadata.updatedAt)
        assertEquals("2026-11-14 04:29:57", summary.metadata.lastPostAt)
        assertTrue(summary.metadata.favorite)
        assertEquals(4, summary.media.size)
    }

    @Test fun listPreservesTrustedOriginalsAndThumbnailsInOrder() {
        val html = """<div class="thread_list"><table><tr id="thread_row_55">
            <td><span class="thread_posts_count">19</span></td><td>
            <div class="thread_text"><a href="/thread/55/">Тема</a></div>
            <div class="thread_images">
            <div class="post_image_block"><a class="expand_image"
            onclick="expand_image2('55_1','/storage/f/a.jpg',event); return false;">
            <img src="/storage/t/a.jpg"></a></div>
            <div class="post_image_block"><a class="expand_image"
            onclick="expand_image2('55_2','https://i.arhivach.vc/storage/f/b.mp4',event); return false;">
            <img src="/storage/t/b.jpg"></a></div>
            <div class="post_image_block"><a class="expand_image"
            onclick="expand_image2('55_3','https://evil.example/storage/f/c.jpg',event); return false;">
            <img src="/storage/t/c.jpg"></a></div></div></td></tr></table></div>"""
        val media =
            requireNotNull(decodeArchiveThreadList(html, "https://arhivach.vc/index/0/", 25))
                .threads
                .single()
                .media
        assertEquals(
            listOf("https://arhivach.vc/storage/f/a.jpg", "https://i.arhivach.vc/storage/f/b.mp4"),
            media.map { it.url },
        )
        assertEquals(
            listOf("https://arhivach.vc/storage/t/a.jpg", "https://arhivach.vc/storage/t/b.jpg"),
            media.map { it.thumbnailUrl },
        )
    }

    @Test fun tagSearchParsesJsonpAndRejectsUnexpectedResponse() {
        val tags = decodeArchiveTags("""pawchanTags({"ok":1,"tags":[{"id":"2","title":"/b/ - Бред"}]});""")
        assertEquals(2L, tags?.single()?.id)
        assertEquals("/b/ - Бред", tags?.single()?.title)
        assertNull(decodeArchiveTags("<html>challenge</html>"))
        assertNull(decodeArchiveTags("other({\"ok\":1,\"tags\":[]})"))
    }

    @Test fun listUsesArchiveThreadIdsAndFiltersUnsafeThumbnail() {
        val html = """<div class="thread_list"><table><tr id="thread_row_55">
            <td><span class="thread_posts_count">19</span></td><td>
            <div class="thread_text"><a href="/thread/55/"><b>Тема</b> — Текст</a></div>
            <div class="thread_images"><img src="https://evil.example/a.png"></div>
            <div class="thread_tags"><a href="/?tags=2">/b/ - Бред</a></div></td></tr></table></div>
            <div class="pagination">
            <a href="/index/25/?tags=2" title="Следующая страница">2</a>
            <a href="/index/1405825/?tags=2" title="Последняя страница">56234</a>
            </div>"""
        val page = requireNotNull(decodeArchiveThreadList(html, "https://arhivach.vc/index/0/?tags=2", 25))
        assertEquals(55L, page.threads.single().id)
        assertEquals(19, page.threads.single().postCount)
        assertEquals(
            2L,
            page.threads
                .single()
                .tags
                .single()
                .id,
        )
        assertNull(page.threads.single().thumbnailUrl)
        assertTrue(page.hasNext)
        assertEquals(56_234, page.totalPages)
        assertNull(decodeArchiveThreadList(html.replace("/thread/55/", "/thread/56/"), "https://arhivach.vc/", 25))
        assertNull(decodeArchiveThreadList("<html>challenge</html>", "https://arhivach.vc/", 25))
    }

    @Test fun archivePostMetadataUsesOnlyFieldsPresentInTheSite() {
        val html = """<div class="post" postid="100">
            <span class="post_num">#1</span><span class="post_time">17/11/26 Втр 00:48:22</span>
            <div class="post_comment_body">OP</div>
            <div class="post_image_block" title="672.6 Кб, 1280 x 960
name.jpg">
            <a class="img_filename" href="/storage/f/a.jpg">name.jpg</a>
            <img src="/storage/t/a.jpg"></div></div>
            <div class="post" postid="101"><span class="post_num">#2</span>
            <span class="poster_sage">sage</span><div class="post_comment_body">Reply</div></div>"""
        val posts = requireNotNull(decodeArchiveBrowserThread(html, "https://arhivach.vc/thread/55/", 55)).posts
        assertTrue(posts[0].details.originalPoster)
        assertFalse(posts[1].details.originalPoster)
        assertTrue(posts[1].details.sage)
        assertEquals(
            java.time.Instant
                .parse("2026-11-16T21:48:22Z")
                .epochSecond,
            posts[0].details.timestamp,
        )
        val media = requireNotNull(posts[0].media.single().details)
        assertEquals("672.6 КБ", media.sizeLabel)
        assertEquals(1280, media.dimensions?.width)
        assertEquals(960, media.dimensions?.height)
    }

    @Test fun archivePostDoesNotInventOpDateOrMediaMetadata() {
        val html = """<div class="post" postid="100"><span class="post_time">unknown</span>
            <div class="post_comment_body">OP</div>
            <div class="post_image_block" title="not metadata">
            <a class="img_filename" href="/storage/f/a.jpg">name.jpg</a></div></div>"""
        val post = requireNotNull(decodeArchiveBrowserThread(html, "https://arhivach.vc/thread/55/", 55)).posts.single()
        assertFalse(post.details.originalPoster)
        assertNull(post.details.timestamp)
        assertNull(post.media.single().details)
    }

    @Test fun archiveTextUsesSharedFormattingAndOnlyVerifiedLocalReferences() {
        val html = """<div class="post" postid="100"><div class="post_comment_body">ОП</div></div>
            <div class="post" postid="101"><div class="post_comment_body">
            <b>bold</b> <span class="unkfunc">&gt;quote</span> <span class="spoiler">hidden</span>
            <a class="post-reply-link" data-num="100" data-thread="100" href="#100">&gt;&gt;100</a>
            <a class="post-reply-link" data-num="102" data-thread="999" href="#102">&gt;&gt;102</a>
            <a href="https://example.org/">website</a></div></div>"""
        val thread = requireNotNull(decodeArchiveBrowserThread(html, "https://arhivach.vc/thread/55/", 55))
        val document = requireNotNull(thread.posts.last().document)
        assertTrue(document.runs.any { PostTextStyle.BOLD in it.styles })
        assertTrue(document.runs.any { PostTextStyle.GREENTEXT in it.styles })
        assertTrue(document.runs.any { PostTextStyle.SPOILER in it.styles })
        val references = document.runs.mapNotNull { it.link as? PostLink.Reference }
        assertEquals(listOf(100L), references.map { it.post.value })
        assertTrue(document.runs.any { it.link is PostLink.External && it.text == "website" })
        assertTrue(document.runs.any { it.text.contains(">>102") && it.link == null })
    }

    @Test fun postTextDoesNotGainWhitespaceFromHtmlFormatting() {
        val html =
            """<div class="post" postid="100"><div class="post_comment_body">ОП</div></div>""" +
                """<div class="post" postid="101"><div class="post_comment_body">""" +
                """<a href="#100" class="post-reply-link" data-thread="100" data-num="100">&gt;&gt;100</a>""" +
                """<br>Текст<br><br>Абзац</div></div>"""
        val thread = requireNotNull(decodeArchiveBrowserThread(html, "https://arhivach.vc/thread/55/", 55))
        assertEquals(">>100\nТекст\n\nАбзац", requireNotNull(thread.posts.last().document).plainText)
    }

    @Test fun threadReadsPostsWithoutRequiringOriginalBoardOrOpIdentity() {
        val html = """<title>Тема — Архивач</title><div class="post" postid="999">
            <span class="poster_name">Аноним</span><span class="post_time">10/10/26</span>
            <span class="post_subject">Настоящая тема</span>
            <div class="post_comment_body">ОП<br>текст</div>
            <div class="post_image_block"><img src="/storage/t/cat.webp">
            <a class="img_filename" href="/storage/c/cat.webp">cat</a></div></div>
            <div class="post"><div class="post_comment_body">Удалённый без номера</div></div>"""
        val thread = requireNotNull(decodeArchiveBrowserThread(html, "https://arhivach.vc/thread/55/", 55))
        assertEquals(55L, thread.id)
        assertEquals("999", thread.posts.first().number)
        assertEquals("Настоящая тема", thread.posts.first().subject)
        assertNull(thread.posts.last().subject)
        assertEquals(
            "https://arhivach.vc/storage/c/cat.webp",
            thread.posts
                .first()
                .media
                .single()
                .url,
        )
        assertNull(thread.posts.last().number)
        assertNull(decodeArchiveBrowserThread("<div>challenge</div>", "https://arhivach.vc/thread/55/", 55))
    }

    @Test fun threadKeepsAllTrustedArchiveAttachmentsAndRejectsOtherHosts() {
        val html = """<title>Тема</title><div class="post" postid="999">
            <div class="post_comment_body">ОП</div>
            <div class="post_image_block"><img src="/storage/t/a.jpg">
            <a class="img_filename" href="/storage/f/a.jpg">a</a></div>
            <div class="post_image_block"><img src="/storage/t/b.thumb">
            <a class="img_filename" href="https://i.arhivach.vc/storage/f/b.mp4">b</a></div>
            <div class="post_image_block"><img src="/storage/t/c.jpg">
            <a class="img_filename" href="https://evil.example/storage/f/c.jpg">c</a></div></div>"""
        val thread = requireNotNull(decodeArchiveBrowserThread(html, "https://arhivach.vc/thread/55/", 55))
        assertEquals(
            listOf("https://arhivach.vc/storage/f/a.jpg", "https://i.arhivach.vc/storage/f/b.mp4"),
            thread.posts
                .single()
                .media
                .map { it.url },
        )
    }

    @Test fun requestsAreReadOnlyAndRejectBadParametersBeforeNetwork() =
        runBlocking {
            val requests = mutableListOf<Request>()
            val client =
                OkHttpClient
                    .Builder()
                    .addInterceptor { chain ->
                        val request = chain.request()
                        requests += request
                        val body =
                            when (request.url.encodedPath) {
                                "/ajax/" -> {
                                    """pawchanTags({"ok":1,"tags":[]})"""
                                }

                                "/index/25/" -> {
                                    """<div class="thread_list"><table></table></div>"""
                                }

                                else -> {
                                    """<title>Тема</title><div class="post">""" +
                                        """<div class="post_comment_body">Текст</div></div>"""
                                }
                            }
                        Response
                            .Builder()
                            .request(request)
                            .protocol(Protocol.HTTP_1_1)
                            .code(200)
                            .message("ok")
                            .body(body.toResponseBody())
                            .build()
                    }.build()
            val api = ArhivachBrowser(client)
            assertEquals(DomainOutcome.Failure(DomainFailure.InvalidResponse), api.loadThread(-1))
            assertEquals(DomainOutcome.Failure(DomainFailure.InvalidResponse), api.loadThreads(listOf(-1), 0))
            assertTrue(requests.isEmpty())
            assertTrue(api.searchTags("бред") is DomainOutcome.Success)
            val page = api.loadThreads(listOf(2), 1) as DomainOutcome.Success
            assertFalse(page.value.hasNext)
            assertTrue(api.loadThread(55) is DomainOutcome.Success)
            assertEquals(3, requests.size)
            assertTrue(
                requests.all {
                    it.method == "GET" && it.url.host == "arhivach.vc" && it.header("Cookie") == null
                },
            )
            assertEquals("2", requests[1].url.queryParameter("tags"))
            client.dispatcher.executorService.shutdown()
        }
}
