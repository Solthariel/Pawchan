package com.solthariel.pawchan.composer

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.solthariel.pawchan.domain.drafts.DraftImageEdit
import com.solthariel.pawchan.domain.drafts.DraftImageSession

/** One controller backs every tool, including crop, drawing, redaction, and text. */
internal class ImageEditorState(
    initial: DraftImageEdit,
) {
    private val session = DraftImageSession(initial)
    var edit by mutableStateOf(session.current)
        private set
    var canUndo by mutableStateOf(false)
        private set
    var canRedo by mutableStateOf(false)
        private set
    var gestureStart by mutableStateOf<DraftImageEdit?>(null)
        private set
    var inGesture by mutableStateOf(false)
        private set

    fun begin() {
        if (session.inGesture) return
        gestureStart = edit
        session.beginGesture()
        sync()
    }

    fun preview(next: DraftImageEdit) {
        session.preview(next)
        sync()
    }

    fun finish() {
        session.finishGesture()
        gestureStart = null
        sync()
    }

    fun cancel() {
        session.cancelGesture()
        gestureStart = null
        sync()
    }

    fun commit(next: DraftImageEdit) {
        if (session.inGesture) return
        session.commit(next)
        sync()
    }

    fun undo() {
        if (session.inGesture) return
        session.undo()
        sync()
    }

    fun redo() {
        if (session.inGesture) return
        session.redo()
        sync()
    }

    fun reset() {
        if (session.inGesture) return
        session.reset()
        sync()
    }

    private fun sync() {
        edit = session.current
        canUndo = session.canUndo && !session.inGesture
        canRedo = session.canRedo && !session.inGesture
        inGesture = session.inGesture
    }
}
