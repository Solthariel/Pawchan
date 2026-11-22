package com.solthariel.pawchan.data.drafts

import com.solthariel.pawchan.domain.drafts.DraftCrop
import com.solthariel.pawchan.domain.drafts.DraftImageEdit
import com.solthariel.pawchan.domain.drafts.DraftImageOptions
import com.solthariel.pawchan.domain.drafts.DraftPoint
import com.solthariel.pawchan.domain.drafts.DraftRedaction
import com.solthariel.pawchan.domain.drafts.DraftRedactionMode
import com.solthariel.pawchan.domain.drafts.DraftRedactionTool
import com.solthariel.pawchan.domain.drafts.DraftShape
import com.solthariel.pawchan.domain.drafts.DraftShapeKind
import com.solthariel.pawchan.domain.drafts.DraftStroke
import com.solthariel.pawchan.domain.drafts.DraftTextAppearance
import com.solthariel.pawchan.domain.drafts.DraftTextMark
import com.solthariel.pawchan.domain.drafts.DraftTextStyle
import com.solthariel.pawchan.domain.drafts.MAX_IMAGE_POINTS
import com.solthariel.pawchan.domain.drafts.MediaDraftOptions
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Test

class DraftImageEditCodecTest {
    @Test
    fun cropTransformAndEveryMarkRoundTripAlongsideOtherOptions() {
        val a = DraftPoint(0.2f, 0.3f)
        val b = DraftPoint(0.7f, 0.9f)
        val edit =
            DraftImageEdit(
                DraftCrop(0.1f, 0.2f, 0.9f, 1f),
                3,
                true,
                listOf(
                    DraftStroke(listOf(a, b), -1, 0.02f, true),
                    DraftShape(a, b, -16777216, 0.01f, DraftShapeKind.ARROW),
                    DraftTextMark("Привет", a, DraftTextAppearance(-1, 0.1f, 45f, DraftTextStyle.BANNER)),
                    DraftRedaction(listOf(a, b), DraftRedactionMode.PIXELS, DraftRedactionTool.RECTANGLE, 0.05f),
                ),
            )
        val source = MediaDraftOptions(uniqueHash = true, image = DraftImageOptions(edit = edit))
        val decoded = decodeMediaOptions(Json.parseToJsonElement(encodeMediaOptions(source).toString()).jsonObject)
        assertEquals(source, decoded)
    }

    @Test
    fun legacyOptionsHaveNoImageEdit() {
        assertNull(decodeMediaOptions(Json.parseToJsonElement("{}").jsonObject).edit)
    }

    @Test
    fun oversizedPathIsRejectedBeforeAllocationOfPoints() {
        val points = List(MAX_IMAGE_POINTS + 1) { "[0.5,0.5]" }.joinToString(",")
        val recipe =
            """{"edit":{"version":1,"crop":[0,0,1,1],"turns":0,"mirror":false,"marks":[""" +
                """{"type":"stroke","points":[$points],"color":-1,"width":0.1,"marker":false}]}}"""
        assertThrows(IllegalArgumentException::class.java) {
            decodeMediaOptions(Json.parseToJsonElement(recipe).jsonObject)
        }
    }

    @Test
    fun invalidOrUnsupportedRecipesAreRejectedInsteadOfSilentlyDropped() {
        listOf(
            """{"edit":null}""",
            """{"edit":{"version":2,"crop":[0,0,1,1],"turns":0,"mirror":false,"marks":[]}}""",
            """{"edit":{"version":1,"crop":[0,0,1,1],"turns":0,"mirror":false,"marks":[{"type":"unknown"}]}}""",
            """{"edit":{"version":1,"crop":[0,0,0,1],"turns":0,"mirror":false,"marks":[]}}""",
        ).forEach { json ->
            assertThrows(RuntimeException::class.java) { decodeMediaOptions(Json.parseToJsonElement(json).jsonObject) }
        }
    }
}
