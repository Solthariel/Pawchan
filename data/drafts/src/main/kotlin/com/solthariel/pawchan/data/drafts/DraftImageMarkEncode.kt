package com.solthariel.pawchan.data.drafts

import com.solthariel.pawchan.domain.drafts.DraftImageMark
import com.solthariel.pawchan.domain.drafts.DraftRedaction
import com.solthariel.pawchan.domain.drafts.DraftShape
import com.solthariel.pawchan.domain.drafts.DraftStroke
import com.solthariel.pawchan.domain.drafts.DraftTextMark
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

internal fun encodeImageMark(mark: DraftImageMark): JsonObject =
    buildJsonObject {
        when (mark) {
            is DraftStroke -> {
                put("type", "stroke")
                put("points", JsonArray(mark.points.map(::encodePoint)))
                put("color", mark.color)
                put("width", mark.width)
                put("marker", mark.marker)
            }

            is DraftShape -> {
                put("type", "shape")
                put("from", encodePoint(mark.from))
                put("to", encodePoint(mark.to))
                put("color", mark.color)
                put("width", mark.width)
                put("kind", mark.kind.name)
            }

            is DraftTextMark -> {
                put("type", "text")
                put("text", mark.text)
                put("anchor", encodePoint(mark.anchor))
                put("color", mark.appearance.color)
                put("size", mark.appearance.size)
                put("angle", mark.appearance.angle)
                put("style", mark.appearance.style.name)
                put("alignment", mark.appearance.alignment.name)
            }

            is DraftRedaction -> {
                put("type", "redaction")
                put("points", JsonArray(mark.points.map(::encodePoint)))
                put("color", mark.opaqueColor)
                put("mode", mark.mode.name)
                put("tool", mark.tool.name)
                put("width", mark.width)
            }
        }
    }
