package com.solthariel.pawchan.data.network

import com.solthariel.pawchan.domain.model.BoardAddress
import com.solthariel.pawchan.domain.model.BoardDescriptor
import com.solthariel.pawchan.domain.model.BoardId
import com.solthariel.pawchan.domain.model.PostLink
import com.solthariel.pawchan.domain.model.PostTextStyle
import com.solthariel.pawchan.domain.model.SiteId
import com.solthariel.pawchan.domain.model.ThreadAddress
import com.solthariel.pawchan.domain.model.ThreadId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class FourChanPayloadDecoderTest {
    private val decoder = FourChanPayloadDecoder(dvachReadJson())
    private val board = BoardAddress(FOUR_CHAN_SITE, requireNotNull(BoardId.parse("g")))
    private val thread = ThreadAddress(board, requireNotNull(ThreadId.parse(100)))

    @Test fun directoryKeepsSiteIdentityAndReadOnlyCapability() {
        val boards =
            requireNotNull(
                decoder.boards(
                    """{"boards":[{"board":"g","title":"Technology","ws_board":1},
          {"board":"b","title":"Random","ws_board":0},{"board":"../evil","title":"bad"}]}""",
                ),
            )
        assertEquals(2, boards.size)
        assertEquals(board, boards.first().address)
        assertFalse(boards.first().postingEnabled)
        assertEquals("Взрослым", boards.last().category)
    }

    @Test fun catalogMapsOpMediaAndDoesNotInventSpeedOrReplyImageTypes() {
        val snapshot =
            requireNotNull(
                decoder.catalog(
                    BoardDescriptor(board, "Technology", true),
                    """[{"page":1,
          "threads":[{"no":100,"resto":0,"com":"hello","time":123,"replies":3,"images":2,
          "filename":"cat","ext":".png","tim":1234,"fsize":123,"w":10,"h":20,"sticky":1}]}]""",
                ),
            )
        val item = snapshot.threads.single()
        assertEquals(thread, item.address)
        assertEquals(3, item.statistics.replies)
        assertEquals(3, item.statistics.files)
        assertEquals(1, item.statistics.images)
        assertFalse(item.statistics.mediaBytesComplete)
        assertFalse(snapshot.boardSpeedAvailable)
        assertEquals(
            "https://i.4cdn.org/g/1234.png",
            item.attachments
                .single()
                .remoteUrl.value,
        )
        assertTrue(item.flags.pinned)
    }

    @Test fun threadMapsReferencesAndRejectsMismatchedThread() {
        val quote =
            """<a href=\"#p100\">&gt;&gt;100</a><br>""" +
                """<span class=\"quote\">&gt;quoted</span>"""
        val json =
            """{"posts":[{"no":100,"resto":0,"name":"Anonymous","com":"op"},""" +
                """{"no":101,"resto":100,"com":"$quote"}]}"""
        val snapshot = requireNotNull(decoder.thread(thread, json))
        assertEquals(2, snapshot.posts.size)
        assertTrue(
            snapshot.posts
                .first()
                .content.metadata.originalPoster,
        )
        val runs =
            snapshot.posts
                .last()
                .content.document.runs
        assertTrue(runs.any { it.link is PostLink.Reference && it.text == ">>100" })
        assertTrue(runs.any { PostTextStyle.GREENTEXT in it.styles && it.text == ">quoted" })
        assertNull(decoder.thread(thread.copy(thread = requireNotNull(ThreadId.parse(999))), json))
        assertNull(decoder.thread(thread, """{"posts":[{"no":100,"resto":0},{"no":101,"resto":999}]}"""))
    }

    @Test fun linksRemainOnFourChanAndUntrustedDomainsAreNotInternalReferences() {
        val doc =
            parseFourChanDocument(
                """<a href="/g/thread/100#p101">&gt;&gt;101</a>
          <a href="https://evil.example/g/thread/100#p101">&gt;&gt;101</a>""",
            )
        val links = doc.runs.mapNotNull { it.link }
        val first = links[0] as PostLink.Reference
        assertEquals(thread, first.thread)
        assertEquals(101L, first.post.value)
        assertTrue(links[1] is PostLink.External)
    }

    @Test fun maliciousAttachmentPathAndWrongSiteAreRejected() {
        val dto =
            FourChanPostDto().apply {
                no = 1
                tim = 1234
                filename = "x"
                ext = "/../../private"
            }
        assertNull(dto.attachment(board))
        val valid =
            FourChanPostDto().apply {
                no = 1
                tim = 1234
                filename = "x"
                ext = ".jpg"
                spoiler = 1
            }
        assertNull(valid.attachment(board))
        dto.ext = ".jpg"
        assertNotNull(dto.attachment(board))
        val wrong = board.copy(site = requireNotNull(SiteId.parse("dvach")))
        assertNull(decoder.catalog(BoardDescriptor(wrong, "bad", true), "[]"))
        assertNull(decoder.thread(thread.copy(board = wrong), """{"posts":[]}"""))
    }
}
