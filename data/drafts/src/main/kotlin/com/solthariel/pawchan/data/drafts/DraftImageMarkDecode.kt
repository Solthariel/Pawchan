package com.solthariel.pawchan.data.drafts

import com.solthariel.pawchan.domain.drafts.DEFAULT_REDACTION_COLOR
import com.solthariel.pawchan.domain.drafts.DraftImageMark
import com.solthariel.pawchan.domain.drafts.DraftRedaction
import com.solthariel.pawchan.domain.drafts.DraftRedactionMode
import com.solthariel.pawchan.domain.drafts.DraftRedactionTool
import com.solthariel.pawchan.domain.drafts.DraftShape
import com.solthariel.pawchan.domain.drafts.DraftShapeKind
import com.solthariel.pawchan.domain.drafts.DraftStroke
import com.solthariel.pawchan.domain.drafts.DraftTextAlignment
import com.solthariel.pawchan.domain.drafts.DraftTextAppearance
import com.solthariel.pawchan.domain.drafts.DraftTextMark
import com.solthariel.pawchan.domain.drafts.DraftTextStyle
import com.solthariel.pawchan.domain.drafts.MAX_IMAGE_POINTS
import kotlinx.serialization.json.JsonObject

internal fun decodeImageMark(root: JsonObject): DraftImageMark =
    when (root.string("type")) {
        "stroke" -> {
            DraftStroke(decodePoints(root), root.integer("color"), root.float("width"), root.requiredBoolean("marker"))
        }

        "shape" -> {
            decodeShape(root)
        }

        "text" -> {
            decodeText(root)
        }

        "redaction" -> {
            DraftRedaction(
                decodePoints(root),
                DraftRedactionMode.valueOf(root.string("mode")),
                DraftRedactionTool.valueOf(root.string("tool")),
                root.float("width"),
                if ("color" in root) root.integer("color") else DEFAULT_REDACTION_COLOR,
            )
        }

        else -> {
            error("Unknown image mark")
        }
    }

private fun decodeShape(root: JsonObject) =
    DraftShape(
        decodePoint(requireNotNull(root["from"])),
        decodePoint(requireNotNull(root["to"])),
        root.integer("color"),
        root.float("width"),
        DraftShapeKind.valueOf(root.string("kind")),
    )

private fun decodeText(root: JsonObject) =
    DraftTextMark(
        root.string("text"),
        decodePoint(requireNotNull(root["anchor"])),
        DraftTextAppearance(
            root.integer("color"),
            root.float("size"),
            root.float("angle"),
            DraftTextStyle.valueOf(root.string("style")),
            DraftTextAlignment.valueOf(root.string("alignment")),
        ),
    )

private fun decodePoints(root: JsonObject) =
    root.array("points").let { values ->
        require(values.size <= MAX_IMAGE_POINTS)
        values.map(::decodePoint)
    }
