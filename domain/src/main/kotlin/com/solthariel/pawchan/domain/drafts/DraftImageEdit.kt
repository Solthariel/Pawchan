package com.solthariel.pawchan.domain.drafts

/** Coordinates are relative to the EXIF-oriented source, before crop or transforms. */
data class DraftPoint(
    val x: Float,
    val y: Float,
) {
    init {
        require(x.isFinite() && x in 0f..1f && y.isFinite() && y in 0f..1f)
    }
}

data class DraftCrop(
    val left: Float = 0f,
    val top: Float = 0f,
    val right: Float = 1f,
    val bottom: Float = 1f,
) {
    init {
        require(listOf(left, top, right, bottom).all { it.isFinite() && it in 0f..1f })
        require(left < right && top < bottom)
    }
}

/** Transform = clockwise quarter-turns after an optional source-space horizontal mirror. */
data class DraftImageEdit(
    val crop: DraftCrop = DraftCrop(),
    val quarterTurns: Int = 0,
    val mirrored: Boolean = false,
    val marks: List<DraftImageMark> = emptyList(),
) {
    init {
        require(quarterTurns in 0..LAST_QUARTER_TURN)
        require(marks.size <= MAX_IMAGE_MARKS && marks.sumOf { it.pointCount } <= MAX_IMAGE_POINTS)
    }

    fun rotateLeft(): DraftImageEdit = copy(quarterTurns = (quarterTurns + LAST_QUARTER_TURN) % QUARTER_TURN_COUNT)

    fun flipHorizontal(): DraftImageEdit =
        copy(quarterTurns = (QUARTER_TURN_COUNT - quarterTurns) % QUARTER_TURN_COUNT, mirrored = !mirrored)

    fun flipVertical(): DraftImageEdit {
        val turns = (THREE_HALVES_TURNS - quarterTurns) % QUARTER_TURN_COUNT
        return copy(quarterTurns = turns, mirrored = !mirrored)
    }
}

const val MAX_IMAGE_MARKS = 128
const val MAX_IMAGE_POINTS = 4096
const val MAX_IMAGE_TEXT_LENGTH = 512

private const val LAST_QUARTER_TURN = 3
private const val QUARTER_TURN_COUNT = 4
private const val THREE_HALVES_TURNS = 6
