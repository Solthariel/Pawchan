package com.solthariel.pawchan.domain.drafts

/** A gesture can produce many previews but commits exactly one undo step. */
class DraftImageSession(
    initial: DraftImageEdit,
) {
    private val history = DraftImageHistory(initial)
    private var gestureStart: DraftImageEdit? = null
    var current: DraftImageEdit = history.current
        private set

    val canUndo: Boolean get() = (gestureStart != null && current != gestureStart) || history.canUndo
    val canRedo: Boolean get() = gestureStart == null && history.canRedo
    val inGesture: Boolean get() = gestureStart != null

    fun beginGesture() {
        check(gestureStart == null)
        gestureStart = history.current
    }

    fun preview(next: DraftImageEdit) {
        check(gestureStart != null)
        current = next.frozenCopy()
    }

    fun finishGesture() {
        check(gestureStart != null)
        history.commit(current)
        current = history.current
        gestureStart = null
    }

    fun cancelGesture() {
        if (gestureStart != null) {
            current = history.current
            gestureStart = null
        }
    }

    fun commit(next: DraftImageEdit) {
        check(gestureStart == null)
        history.commit(next)
        current = history.current
    }

    fun undo() {
        if (gestureStart != null) {
            cancelGesture()
        } else {
            history.undo()
            current = history.current
        }
    }

    fun redo() {
        if (gestureStart == null) {
            history.redo()
            current = history.current
        }
    }

    fun reset() = commit(DraftImageEdit())
}
