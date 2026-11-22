package com.solthariel.pawchan.domain.drafts

/** Every update is based on the original edit so a gesture adds exactly one mark. */
class DraftMarkGesture private constructor(
    private val original: DraftImageEdit,
    private val tool: DraftMarkTool,
    initial: DraftPoint,
    private val pointBudget: Int,
) {
    private val points = mutableListOf(initial)
    var limitReached: Boolean = false
        private set

    fun update(point: DraftPoint): DraftImageEdit {
        if (points.last() != point) {
            if (isTwoPointTool()) {
                if (points.size == 1) points.add(point) else points[1] = point
            } else if (points.size < pointBudget) {
                points.add(point)
            } else {
                limitReached = true
            }
        }
        return snapshot()
    }

    fun snapshot(): DraftImageEdit = original.copy(marks = original.marks + mark())

    private fun mark(): DraftImageMark =
        when (val selected = tool) {
            is DraftMarkTool.Pen -> {
                DraftStroke(points.toList(), selected.color, selected.width, selected.marker)
            }

            is DraftMarkTool.Shape -> {
                DraftShape(points.first(), points.last(), selected.color, selected.width, selected.kind)
            }

            is DraftMarkTool.Redact -> {
                val rectangle = selected.tool == DraftRedactionTool.RECTANGLE
                val path = if (rectangle) listOf(points.first(), points.last()) else points.toList()
                DraftRedaction(path, selected.mode, selected.tool, selected.width, selected.color)
            }
        }

    private fun isTwoPointTool(): Boolean =
        tool is DraftMarkTool.Shape || (tool is DraftMarkTool.Redact && tool.tool == DraftRedactionTool.RECTANGLE)

    companion object {
        fun begin(
            edit: DraftImageEdit,
            tool: DraftMarkTool,
            initial: DraftPoint,
        ): DraftMarkGesture? {
            val remaining = MAX_IMAGE_POINTS - edit.marks.sumOf { it.pointCount }
            if (edit.marks.size >= MAX_IMAGE_MARKS || remaining < MIN_GESTURE_POINTS) return null
            return DraftMarkGesture(edit.frozenCopy(), tool, initial, remaining).also { it.snapshot() }
        }
    }
}

private const val MIN_GESTURE_POINTS = 2
