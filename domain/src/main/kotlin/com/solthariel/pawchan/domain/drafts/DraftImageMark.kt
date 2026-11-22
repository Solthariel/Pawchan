package com.solthariel.pawchan.domain.drafts

sealed interface DraftImageMark {
    val pointCount: Int
}

data class DraftStroke(
    val points: List<DraftPoint>,
    val color: Int,
    val width: Float,
    val marker: Boolean = false,
) : DraftImageMark {
    init {
        require(points.isNotEmpty() && points.size <= MAX_IMAGE_POINTS)
        requireValidWidth(width)
    }

    override val pointCount: Int get() = points.size
}

enum class DraftShapeKind { ARROW, RECTANGLE, OVAL }

data class DraftShape(
    val from: DraftPoint,
    val to: DraftPoint,
    val color: Int,
    val width: Float,
    val kind: DraftShapeKind,
) : DraftImageMark {
    init {
        requireValidWidth(width)
    }

    override val pointCount: Int get() = 2
}

enum class DraftTextStyle { MEME, BANNER }

enum class DraftTextAlignment { LEFT, CENTER, RIGHT }

data class DraftTextAppearance(
    val color: Int,
    val size: Float,
    val angle: Float = 0f,
    val style: DraftTextStyle = DraftTextStyle.MEME,
    val alignment: DraftTextAlignment = DraftTextAlignment.CENTER,
) {
    init {
        require(size.isFinite() && size > 0f && size <= 1f)
        require(angle.isFinite() && angle in -FULL_TURN_DEGREES..FULL_TURN_DEGREES)
    }
}

data class DraftTextMark(
    val text: String,
    val anchor: DraftPoint,
    val appearance: DraftTextAppearance,
) : DraftImageMark {
    init {
        require(text.isNotBlank() && text.length <= MAX_IMAGE_TEXT_LENGTH)
    }

    override val pointCount: Int get() = 1
}

enum class DraftRedactionMode { FILL, PIXELS }

enum class DraftRedactionTool { RECTANGLE, BRUSH }

data class DraftRedaction(
    val points: List<DraftPoint>,
    val mode: DraftRedactionMode,
    val tool: DraftRedactionTool,
    val width: Float,
    val color: Int = DEFAULT_REDACTION_COLOR,
) : DraftImageMark {
    val opaqueColor: Int get() = color or DEFAULT_REDACTION_COLOR

    init {
        require(points.isNotEmpty() && points.size <= MAX_IMAGE_POINTS)
        require(tool != DraftRedactionTool.RECTANGLE || points.size == 2)
        requireValidWidth(width)
    }

    override val pointCount: Int get() = points.size
}

private fun requireValidWidth(width: Float) {
    require(width.isFinite() && width > 0f && width <= 1f)
}

private const val FULL_TURN_DEGREES = 360f

/** Default black also supplies the opaque alpha mask for every fill color. */
const val DEFAULT_REDACTION_COLOR = -16777216
