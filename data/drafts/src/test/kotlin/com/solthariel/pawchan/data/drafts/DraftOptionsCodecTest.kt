package com.solthariel.pawchan.data.drafts

import com.solthariel.pawchan.domain.drafts.DraftImageReencoding
import com.solthariel.pawchan.domain.drafts.MediaDraftOptions
import com.solthariel.pawchan.domain.posting.PostingOptions
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DraftOptionsCodecTest {
    @Test fun legacyStickerKeyIsIgnoredAndNoLongerWritten() {
        val legacy =
            Json.parseToJsonElement(
                """{"name":"Neko","sage":true,"author":false,"tag":"t",""" +
                    """"sticker":{"id":"DH9DioJJ_97uXUsf3","thumbnail":"https://2ch.su/stickers/thumb.png"}}""",
            ) as JsonObject
        val options = decodePostingOptions(legacy)
        assertEquals(PostingOptions("Neko", sage = true, tag = "t"), options)
        assertTrue("sticker" !in encodePostingOptions(options))
    }

    @Test fun exactScaleDivisorSurvivesForActiveAndPreviousSettings() {
        listOf(1, 2, 4, 8).forEach { divisor ->
            val settings = DraftImageReencoding(scaleDivisor = divisor)
            val options = MediaDraftOptions(reencoding = settings, previousReencoding = settings)
            assertEquals(options, decodeMediaOptions(encodeMediaOptions(options)))
        }
    }

    @Test fun legacyPercentageDraftKeepsItsScale() {
        val legacy =
            Json.parseToJsonElement(
                """{"reencoding":{"jpeg":true,"quality":87,"scale":60}}""",
            ) as JsonObject
        val settings = requireNotNull(decodeMediaOptions(legacy).reencoding)
        assertEquals(1, settings.scaleDivisor)
        assertEquals(0.6f, settings.scaleFactor, 0f)
    }
}
