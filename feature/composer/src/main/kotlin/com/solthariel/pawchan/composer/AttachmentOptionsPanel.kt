package com.solthariel.pawchan.composer

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.solthariel.pawchan.feature.composer.R

@Composable
internal fun AttachmentOptionsPanel(
    file: ComposerAttachment,
    enabled: Boolean,
    modifier: Modifier = Modifier,
    onEditorAction: ((Int) -> Unit)? = null,
    editorState: AttachmentOptionsEditorState = AttachmentOptionsEditorState(),
    onUpdate: (ComposerAttachment) -> Unit,
) {
    val items = attachmentOptionLabels(file, onEditorAction != null)
    val nameActive = editorState.namePanelOpen || file.options.customName != null || file.options.removeFileName
    AttachmentControlRow(items.size, modifier, centeredLikeSix = onEditorAction != null && file.video) {
        items.forEach { label ->
            key(label) {
                if (
                    onEditorAction != null &&
                    label in setOf(R.string.attachment_edit_image, R.string.attachment_rename)
                ) {
                    AttachmentOptionTile(
                        AttachmentTileState(
                            label,
                            label == R.string.attachment_rename && nameActive,
                            enabled && (label != R.string.attachment_edit_image || editorState.imageEditEnabled),
                            action = true,
                        ),
                        Modifier.weight(1f),
                        { onEditorAction(label) },
                    )
                } else {
                    val onOtherAction = onEditorAction?.let { action -> { action(label) } }
                    AttachmentPanelItem(file, label, enabled, Modifier.weight(1f), onUpdate, onOtherAction)
                }
            }
        }
    }
}

private fun attachmentOptionLabels(
    file: ComposerAttachment,
    inEditor: Boolean,
): List<Int> =
    buildList {
        if (inEditor && file.canReencode) add(R.string.attachment_edit_image)
        if (file.format != null) add(R.string.attachment_unique_hash)
        if (file.canStripMetadata || file.options.edit != null) add(R.string.attachment_strip_metadata)
        // The editor has its own remove-name button next to the text field.
        if (!inEditor) add(R.string.attachment_remove_name)
        add(R.string.markup_spoiler)
        if (file.canReencode) add(R.string.attachment_reencode)
        add(R.string.attachment_rename)
    }

@Composable
private fun AttachmentPanelItem(
    file: ComposerAttachment,
    label: Int,
    enabled: Boolean,
    modifier: Modifier,
    onUpdate: (ComposerAttachment) -> Unit,
    onOtherAction: (() -> Unit)?,
) {
    when (label) {
        R.string.attachment_reencode -> {
            AttachmentReencodingOption(file, enabled, modifier, onUpdate, onOtherAction)
        }

        R.string.attachment_rename -> {
            AttachmentRenameOption(file, enabled, modifier, onUpdate, onOtherAction != null)
        }

        else -> {
            AttachmentOption(
                label,
                attachmentOptionChecked(file, label),
                enabled && !attachmentOptionForced(file, label),
                modifier,
            ) { value ->
                onOtherAction?.invoke()
                val options = file.options
                val updated =
                    when (label) {
                        R.string.attachment_unique_hash -> options.copy(uniqueHash = value)
                        R.string.attachment_strip_metadata -> options.copy(removeMetadata = value)
                        R.string.attachment_remove_name -> options.copy(removeFileName = value)
                        else -> options.copy(spoiler = value)
                    }
                onUpdate(file.copy(options = updated))
            }
        }
    }
}

internal fun attachmentOptionChecked(
    file: ComposerAttachment,
    label: Int,
): Boolean =
    when (label) {
        R.string.attachment_unique_hash -> file.options.uniqueHash || file.options.edit != null
        R.string.attachment_strip_metadata -> file.options.removeMetadata || file.options.edit != null
        R.string.attachment_remove_name -> file.options.removeFileName
        else -> file.options.spoiler
    }

@Composable
internal fun AttachmentOption(
    label: Int,
    checked: Boolean,
    enabled: Boolean,
    modifier: Modifier = Modifier,
    onChange: (Boolean) -> Unit,
) {
    AttachmentOptionTile(AttachmentTileState(label, checked, enabled), modifier) { onChange(!checked) }
}

@Composable
private fun AttachmentReencodingOption(
    file: ComposerAttachment,
    enabled: Boolean,
    modifier: Modifier,
    onUpdate: (ComposerAttachment) -> Unit,
    onOpen: (() -> Unit)?,
) {
    var settingsOpen by remember(file.id) { mutableStateOf(false) }
    AttachmentOptionTile(
        AttachmentTileState(R.string.attachment_reencode, file.options.reencoding != null, enabled, action = true),
        modifier,
    ) {
        onOpen?.invoke()
        settingsOpen = true
    }
    if (settingsOpen) {
        AttachmentReencodingDialog(file, enabled, onUpdate) { settingsOpen = false }
    }
}

@Composable
private fun AttachmentRenameOption(
    file: ComposerAttachment,
    enabled: Boolean,
    modifier: Modifier,
    onUpdate: (ComposerAttachment) -> Unit,
    combinedName: Boolean,
) {
    var open by remember(file.id) { mutableStateOf(false) }
    val originalBase = file.source.fileName.substringBeforeLast('.', file.source.fileName)
    val renamed =
        file.options.customName != null && file.options.customName != originalBase && !file.options.removeFileName
    AttachmentOptionTile(
        AttachmentTileState(
            R.string.attachment_rename,
            renamed || (combinedName && file.options.removeFileName),
            enabled && (combinedName || !file.options.removeFileName),
            action = true,
        ),
        modifier,
    ) { open = true }
    if (open) AttachmentRenameDialog(file, enabled, onUpdate) { open = false }
}

internal fun attachmentOptionForced(
    file: ComposerAttachment,
    label: Int,
): Boolean {
    val forced = setOf(R.string.attachment_unique_hash, R.string.attachment_strip_metadata)
    return file.options.edit != null && label in forced
}
