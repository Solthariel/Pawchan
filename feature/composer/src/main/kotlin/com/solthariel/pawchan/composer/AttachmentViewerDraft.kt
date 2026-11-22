package com.solthariel.pawchan.composer

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.solthariel.pawchan.domain.drafts.DraftImageEdit
import com.solthariel.pawchan.domain.drafts.MediaDraftOptions

/** A viewer session stages edits until Back validates and applies them to the message draft. */
internal class AttachmentViewerDraft(
    private val initial: MediaDraftOptions,
) {
    var options by mutableStateOf(initial)
    val editor = ImageEditorState(initial.edit ?: DraftImageEdit())
    val tools = ImageToolSelection()
    var tab by mutableStateOf(AttachmentEditorTab.FILE)
        private set
    var cropOriginalInverted by mutableStateOf(false)
    var cropPreset by mutableStateOf(ImageCropPreset.FREE)
    var selectedTextIndex by mutableStateOf<Int?>(null)
    var comparing by mutableStateOf(false)

    val currentOptions: MediaDraftOptions
        get() {
            val edit = editor.edit.takeUnless { it == DraftImageEdit() }
            return options.copy(image = options.image.copy(edit = edit))
        }
    val changed: Boolean get() = currentOptions != initial

    // Saving validates raster export before publishing the staged options.
    val canSave: Boolean get() = !editor.inGesture

    fun selectTab(value: AttachmentEditorTab) {
        selectedTextIndex = null
        comparing = false
        tab = value
    }

    fun closeNamePanel() {
        if (tab == AttachmentEditorTab.NAME) selectTab(AttachmentEditorTab.FILE)
    }

    fun undo() {
        closeNamePanel()
        selectedTextIndex = null
        editor.undo()
        cropOriginalInverted = false
        cropPreset = ImageCropPreset.FREE
    }

    fun redo() {
        closeNamePanel()
        selectedTextIndex = null
        editor.redo()
        cropOriginalInverted = false
        cropPreset = ImageCropPreset.FREE
    }

    fun reset() {
        closeNamePanel()
        selectedTextIndex = null
        editor.reset()
        cropOriginalInverted = false
        cropPreset = ImageCropPreset.FREE
    }
}
