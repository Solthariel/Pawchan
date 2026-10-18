package com.solthariel.pawchan.data.network

import com.solthariel.pawchan.domain.model.BoardAddress
import com.solthariel.pawchan.domain.model.BoardId
import com.solthariel.pawchan.domain.model.SiteId
import com.solthariel.pawchan.domain.model.ThreadAddress
import com.solthariel.pawchan.domain.model.ThreadId
import com.solthariel.pawchan.domain.posting.NewThreadDraft
import com.solthariel.pawchan.domain.posting.PostingOptions
import com.solthariel.pawchan.domain.posting.ReplyAttachment
import com.solthariel.pawchan.domain.posting.ReplyDraft
import okhttp3.MultipartBody
import okhttp3.Request
import okio.Buffer
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PostingRequestFactoryTest {
    private val board = BoardAddress(requireNotNull(SiteId.parse("dvach")), requireNotNull(BoardId.parse("biz")))
    private val files =
        listOf(
            ReplyAttachment("first.png", "image/png", byteArrayOf(1, 2)),
            ReplyAttachment("renamed.mp4", "video/mp4", byteArrayOf(3, 4)),
        )

    @Test
    fun replySendsEveryFileInOrderAndPreservesEmail() {
        val address = ThreadAddress(board, requireNotNull(ThreadId.parse(123)))
        val request = PostingRequestFactory.reply(address, ReplyDraft("[b]text[/b]", "token", files, "sage"))
        val body = requireNotNull(request.body) as MultipartBody
        val serialized = Buffer().also(body::writeTo).readUtf8()
        assertEquals(2, body.parts.count { it.headers?.get("Content-Disposition")?.contains("file[]") == true })
        assertTrue(serialized.indexOf("first.png") < serialized.indexOf("renamed.mp4"))
        assertTrue(serialized.contains("sage"))
        assertTrue(serialized.contains("[b]text[/b]"))
        assertTrue(serialized.contains("emoji_captcha_id"))
    }

    @Test
    fun newThreadUsesSameMultiFileWireContract() {
        val request = PostingRequestFactory.newThread(board, NewThreadDraft("subject", "comment", null, files, "mail"))
        val body = requireNotNull(request.body) as MultipartBody
        val serialized = Buffer().also(body::writeTo).readUtf8()
        assertEquals(2, body.parts.count { it.headers?.get("Content-Disposition")?.contains("file[]") == true })
        assertTrue(serialized.contains("mail"))
        assertTrue(serialized.contains("subject"))
    }

    @Test
    fun mixedReplyUsesZeroBasedSpoilerFlagsWithoutChangingFileBodies() {
        val address = ThreadAddress(board, requireNotNull(ThreadId.parse(123)))
        val selected = files.mapIndexed { index, file -> file.copy(spoiler = index == 1) }
        val request = PostingRequestFactory.reply(address, ReplyDraft("reply", null, selected))
        val flags = fields(request)
        assertEquals("1", flags["file_1_nsfw"])
        assertFalse(flags.containsKey("file_0_nsfw"))
        assertFalse(flags.containsKey("spoiler[]"))
        val body = requireNotNull(request.body) as MultipartBody
        val uploaded = body.parts.filter { it.headers?.get("Content-Disposition")?.contains("file[]") == true }
        assertEquals(2, uploaded.size)
        assertArrayEquals(files[1].content, Buffer().also(uploaded[1].body::writeTo).readByteArray())
    }

    @Test
    fun newThreadSpoilerIndexesIncludeAllEightFilesNotOnlyFlaggedOnes() {
        val selected =
            List(8) { index ->
                ReplyAttachment(
                    "$index.png",
                    "image/png",
                    byteArrayOf(index.toByte()),
                    spoiler = index in setOf(0, 3, 7),
                )
            }
        val request = PostingRequestFactory.newThread(board, NewThreadDraft("subject", "comment", null, selected))
        val flags = fields(request).filterKeys { it.endsWith("_nsfw") }
        assertEquals(mapOf("file_0_nsfw" to "1", "file_3_nsfw" to "1", "file_7_nsfw" to "1"), flags)
    }

    @Test
    fun ordinaryFilesDoNotSendAnySpoilerFields() {
        val request = PostingRequestFactory.newThread(board, NewThreadDraft("subject", "comment", null, files))
        assertTrue(fields(request).keys.none { it.endsWith("_nsfw") })
    }

    @Test
    fun selectedIconIsSentForBothReplyAndNewThreadAndAbsentByDefault() {
        val address = ThreadAddress(board, requireNotNull(ThreadId.parse(123)))
        val options = PostingOptions(icon = 7)
        val reply = PostingRequestFactory.reply(address, ReplyDraft("text", null, files, options = options))
        val newDraft = NewThreadDraft("topic", "text", null, files, options = options)
        val thread = PostingRequestFactory.newThread(board, newDraft)
        assertEquals("7", fields(reply)["icon"])
        assertEquals("7", fields(thread)["icon"])
        assertFalse(fields(PostingRequestFactory.reply(address, ReplyDraft("text", null, files))).containsKey("icon"))
    }

    @Test
    fun rulesReadTheSelectedBoardNotAHardcodedExample() {
        assertEquals("/biz/index.json", PostingRequestFactory.boardRules(board).url.encodedPath)
    }

    @Test
    fun sendsNameSageAndServerVerifiedAuthorMark() {
        val address = ThreadAddress(board, requireNotNull(ThreadId.parse(123)))
        val draft =
            ReplyDraft(
                "reply",
                null,
                files,
                "mail@example.test",
                PostingOptions(name = "Neko", sage = true, threadAuthor = true),
            )
        val body = requireNotNull(PostingRequestFactory.reply(address, draft).body) as MultipartBody
        val fields =
            body.parts
                .filter { it.headers?.get("Content-Disposition")?.contains("file[]") != true }
                .associate { part ->
                    val name =
                        requireNotNull(part.headers?.get("Content-Disposition"))
                            .substringAfter("name=\"")
                            .substringBefore('"')
                    name to Buffer().also(part.body::writeTo).readUtf8()
                }
        assertEquals("Neko", fields["name"])
        assertEquals("sage", fields["email"])
        assertEquals("1", fields["op_mark"])
    }

    @Test
    fun uncheckedSageKeepsEmailAndDoesNotSendAuthorMark() {
        val options = PostingOptions(name = "Neko")
        val draft = NewThreadDraft("subject", "comment", null, files, "mail@example.test", options)
        val serialized =
            Buffer()
                .also { requireNotNull(PostingRequestFactory.newThread(board, draft).body).writeTo(it) }
                .readUtf8()
        assertTrue(serialized.contains("mail@example.test"))
        assertTrue(serialized.contains("Neko"))
        assertTrue(!serialized.contains("op_mark"))
    }

    @Test
    fun newThreadSendsTagWithoutStickerField() {
        val draft = NewThreadDraft("subject", "comment", null, files, options = PostingOptions(tag = "test"))
        val request = PostingRequestFactory.newThread(board, draft)
        assertEquals("test", fields(request)["tags"])
        assertTrue("sticker0" !in fields(request))
    }

    private fun fields(request: Request): Map<String, String> {
        val body = requireNotNull(request.body) as MultipartBody
        val regularFields = body.parts.filter { it.headers?.get("Content-Disposition")?.contains("file[]") != true }
        return regularFields.associate { part ->
            val header = requireNotNull(part.headers?.get("Content-Disposition"))
            val name = header.substringAfter("name=\"").substringBefore('"')
            name to Buffer().also(part.body::writeTo).readUtf8()
        }
    }
}
