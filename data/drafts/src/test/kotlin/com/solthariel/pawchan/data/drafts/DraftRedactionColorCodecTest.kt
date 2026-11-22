package com.solthariel.pawchan.data.drafts

import com.solthariel.pawchan.domain.drafts.DraftPoint
import com.solthariel.pawchan.domain.drafts.DraftRedaction
import com.solthariel.pawchan.domain.drafts.DraftRedactionMode
import com.solthariel.pawchan.domain.drafts.DraftRedactionTool
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class DraftRedactionColorCodecTest {
    private val mask =
        DraftRedaction(listOf(DraftPoint(.5f, .5f)), DraftRedactionMode.FILL, DraftRedactionTool.BRUSH, .1f, -65536)

    @Test fun colorSurvivesSavedDraftRoundTrip() {
        assertEquals(mask, decodeImageMark(encodeImageMark(mask)))
    }

    @Test fun legacyDraftsWithoutAColorRemainBlack() {
        val encoded = kotlinx.serialization.json.JsonObject(encodeImageMark(mask).filterKeys { it != "color" })
        assertEquals(mask.copy(color = -16777216), decodeImageMark(encoded))
    }

    @Test fun corruptColorIsRejectedRatherThanSilentlyLosingTheMask() {
        val encoded =
            kotlinx.serialization.json.JsonObject(
                encodeImageMark(mask) + ("color" to kotlinx.serialization.json.JsonNull),
            )
        assertThrows(RuntimeException::class.java) { decodeImageMark(encoded) }
    }
}
