package com.solthariel.pawchan.data.network

import com.solthariel.pawchan.domain.model.BoardAddress
import com.solthariel.pawchan.domain.model.BoardId
import com.solthariel.pawchan.domain.model.MediaKind
import com.solthariel.pawchan.domain.model.SiteId
import com.solthariel.pawchan.domain.model.ThreadAddress
import com.solthariel.pawchan.domain.model.ThreadId
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class StickerAttachmentDecoderTest {
    private val json =
        Json {
            ignoreUnknownKeys = true
            explicitNulls = false
        }
    private val address =
        ThreadAddress(
            BoardAddress(requireNotNull(SiteId.parse("dvach")), requireNotNull(BoardId.parse("test"))),
            requireNotNull(ThreadId.parse(230854L)),
        )

    @Test
    fun keepsRealOpGifStickerWithEmptyMd5() {
        val attachment = decode(payload()).single()

        assertEquals("/stickers/qkRFxT86/osoJgf2g.gif", attachment.id.value)
        assertEquals(MediaKind.ANIMATION, attachment.kind)
        assertTrue(attachment.isSticker)
        assertEquals("https://2ch.su/stickers/qkRFxT86/osoJgf2g.gif", attachment.remoteUrl.value)
        assertEquals("https://2ch.su/stickers/qkRFxT86/osoJgf2g_thumb.gif", attachment.thumbnailUrl?.value)
        assertEquals(192, attachment.dimensions?.width)
        assertEquals(489L * 1024L, attachment.byteSize?.value)
    }

    @Test
    fun usesPathForWhitespaceMd5() {
        val attachment = decode(payload().replace("\"md5\": \"\"", "\"md5\": \"   \"")).single()

        assertEquals("/stickers/qkRFxT86/osoJgf2g.gif", attachment.id.value)
    }

    @Test
    fun usesPathForNullMd5() {
        val attachment = decode(payload().replace("\"md5\": \"\"", "\"md5\": null")).single()

        assertEquals("/stickers/qkRFxT86/osoJgf2g.gif", attachment.id.value)
    }

    @Test
    fun preservesValidMd5Identity() {
        val attachment = decode(payload().replace("\"md5\": \"\"", "\"md5\": \"abc123\"")).single()

        assertEquals("abc123", attachment.id.value)
    }

    @Test
    fun alsoKeepsOrdinaryImagesWithoutMd5() {
        val attachment = decode(payload().replace("osoJgf2g.gif", "osoJgf2g.png")).single()

        assertEquals(MediaKind.IMAGE, attachment.kind)
    }

    @Test
    fun rejectsMissingOriginalPath() {
        val broken = payload().replace("\"path\": \"/stickers/qkRFxT86/osoJgf2g.gif\"", "\"path\": \"\"")

        assertTrue(decode(broken).isEmpty())
    }

    @Test
    fun catalogUsesTheSameStickerFallback() {
        val catalogPayload = """{"board":{"id":"test","name":"Тест"},"threads":[{
            "num":230854,"files_count":1,"files":[{"md5":"","name":"osoJgf2g.gif",
            "path":"/stickers/qkRFxT86/osoJgf2g.gif",
            "thumbnail":"/stickers/qkRFxT86/osoJgf2g_thumb.gif"}]}]}"""
        val attachment =
            requireNotNull(CatalogPayloadDecoder(json).decode(catalogPayload))
                .threads
                .single()
                .attachments
                .single()

        assertEquals(MediaKind.ANIMATION, attachment.kind)
        assertTrue(attachment.isSticker)
        assertEquals("/stickers/qkRFxT86/osoJgf2g.gif", attachment.id.value)
    }

    private fun decode(payload: String) =
        requireNotNull(ThreadPayloadDecoder(json).decode(address, payload))
            .posts
            .first()
            .attachments

    private fun payload(): String {
        val fixture = requireNotNull(javaClass.getResource("/fixtures/thread-sticker-230854.json"))
        return fixture.readText()
    }
}
