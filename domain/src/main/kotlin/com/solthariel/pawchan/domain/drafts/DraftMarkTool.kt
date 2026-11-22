package com.solthariel.pawchan.domain.drafts

/** A gesture records its color and width when it begins. */
sealed interface DraftMarkTool {
    data class Pen(
        val color: Int,
        val width: Float,
        val marker: Boolean = false,
    ) : DraftMarkTool

    data class Shape(
        val color: Int,
        val width: Float,
        val kind: DraftShapeKind,
    ) : DraftMarkTool

    data class Redact(
        val width: Float,
        val mode: DraftRedactionMode,
        val tool: DraftRedactionTool,
        val color: Int = DEFAULT_REDACTION_COLOR,
    ) : DraftMarkTool
}
