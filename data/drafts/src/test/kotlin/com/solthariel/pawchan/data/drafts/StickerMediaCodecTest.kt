package com.solthariel.pawchan.data.drafts

import com.solthariel.pawchan.domain.model.AttachmentId
import com.solthariel.pawchan.domain.model.AttachmentSource
import com.solthariel.pawchan.domain.model.MediaAttachment
import com.solthariel.pawchan.domain.model.MediaKind
import com.solthariel.pawchan.domain.model.RemoteUrl
import kotlinx.serialization.json.JsonObject
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class StickerMediaCodecTest {
    private fun media(
        url: String,
        sticker: Boolean = false,
    ) = MediaAttachment(
        requireNotNull(AttachmentId.parse("file")),
        "file.png",
        AttachmentSource(requireNotNull(RemoteUrl.parse(url)), null),
        null,
        MediaKind.IMAGE,
        isSticker = sticker,
    )

    @Test fun stickerMarkerRoundTripsForSavedCopies() {
        val file = media("https://2ch.hk/stickers/pack/file.png", true)
        assertTrue(file.encodeMedia().decodeMedia().isSticker)
    }

    @Test fun oldDvachCopiesRecoverStickerMarkerWithoutChangingOrdinaryImages() {
        val sticker = media("https://2ch.hk/stickers/pack/file.png").encodeMedia()
        val old = JsonObject(sticker.filterKeys { it != "sticker" })
        assertTrue(old.decodeMedia().isSticker)
        val ordinary = media("https://2ch.hk/b/src/file.png").encodeMedia()
        assertFalse(JsonObject(ordinary.filterKeys { it != "sticker" }).decodeMedia().isSticker)
        val other = media("https://example.org/stickers/file.png").encodeMedia()
        assertFalse(JsonObject(other.filterKeys { it != "sticker" }).decodeMedia().isSticker)
    }
}
