package com.solthariel.pawchan.domain.drafts

import java.util.Collections

/** One entry per completed gesture or tool action; the reset operation is an ordinary commit. */
class DraftImageHistory(
    initial: DraftImageEdit,
    private val capacity: Int = DEFAULT_HISTORY_CAPACITY,
) {
    private val past = mutableListOf<DraftImageEdit>()
    private val future = mutableListOf<DraftImageEdit>()
    var current: DraftImageEdit = initial.frozenCopy()
        private set

    init {
        require(capacity in 1..DEFAULT_HISTORY_CAPACITY)
    }

    val canUndo: Boolean get() = past.isNotEmpty()
    val canRedo: Boolean get() = future.isNotEmpty()

    fun commit(next: DraftImageEdit) {
        if (next == current) return
        past.add(current)
        if (past.size > capacity) past.removeAt(0)
        current = next.frozenCopy()
        future.clear()
    }

    fun undo() {
        if (past.isEmpty()) return
        future.add(current)
        current = past.removeAt(past.lastIndex)
    }

    fun redo() {
        if (future.isEmpty()) return
        past.add(current)
        current = future.removeAt(future.lastIndex)
    }
}

private const val DEFAULT_HISTORY_CAPACITY = 50

internal fun DraftImageEdit.frozenCopy(): DraftImageEdit =
    copy(
        marks =
            Collections.unmodifiableList(
                marks.map { mark ->
                    when (mark) {
                        is DraftStroke -> mark.copy(points = Collections.unmodifiableList(mark.points.toList()))
                        is DraftRedaction -> mark.copy(points = Collections.unmodifiableList(mark.points.toList()))
                        is DraftShape, is DraftTextMark -> mark
                    }
                },
            ),
    )
