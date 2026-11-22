package com.solthariel.pawchan.domain.drafts

/** Translation is measured on the cropped output canvas; angle is always in output space. */
class DraftTextGesture(
    edit: DraftImageEdit,
    private val index: Int,
) {
    private val initial = edit.frozenCopy()
    private val text = initial.marks.getOrNull(index) as? DraftTextMark
    private val position = text?.let { initial.sourceToCanvas(it.anchor) }

    init {
        require(text != null && position != null) { "Текстовая метка не найдена" }
    }

    fun update(
        delta: ImageCanvasPoint,
        scale: Float,
        rotationDegrees: Float,
    ): DraftImageEdit {
        require(delta.x.isFinite() && delta.y.isFinite())
        require(scale.isFinite() && scale > 0f && rotationDegrees.isFinite())
        val original = requireNotNull(text)
        val origin = requireNotNull(position)
        val point = ImageCanvasPoint((origin.x + delta.x).coerceIn(0f, 1f), (origin.y + delta.y).coerceIn(0f, 1f))
        val anchor = requireNotNull(initial.canvasToSource(point))
        val angle = normalizeTextAngle(original.appearance.angle.toDouble() + rotationDegrees.toDouble())
        val size = (original.appearance.size.toDouble() * scale.toDouble()).coerceIn(MIN_TEXT_SIZE, 1.0).toFloat()
        val updated = original.copy(anchor = anchor, appearance = original.appearance.copy(size = size, angle = angle))
        return initial.copy(marks = initial.marks.mapIndexed { i, mark -> if (i == index) updated else mark })
    }
}

private fun normalizeTextAngle(degrees: Double): Float {
    val wrapped = ((degrees + HALF_TURN) % FULL_TURN + FULL_TURN) % FULL_TURN
    return (wrapped - HALF_TURN).toFloat()
}

private const val HALF_TURN = 180.0
private const val FULL_TURN = 360.0
private const val MIN_TEXT_SIZE = 0.001
