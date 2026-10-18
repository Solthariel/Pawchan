package com.solthariel.pawchan.composer

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.solthariel.pawchan.feature.composer.R

@Composable
internal fun AttachmentOptionsPanel(
    file: ComposerAttachment,
    enabled: Boolean,
    modifier: Modifier = Modifier,
    onUpdate: (ComposerAttachment) -> Unit,
) {
    val items =
        buildList {
            if (file.format != null) add(R.string.attachment_unique_hash)
            if (file.canStripMetadata) add(R.string.attachment_strip_metadata)
            add(R.string.attachment_remove_name)
            add(R.string.markup_spoiler)
            if (file.canReencode) add(R.string.attachment_reencode)
            add(R.string.attachment_rename)
        }
    Row(
        modifier.fillMaxWidth().padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        items.forEach { label ->
            AttachmentPanelItem(file, label, enabled, Modifier.weight(1f), onUpdate)
        }
    }
}

@Composable
private fun AttachmentPanelItem(
    file: ComposerAttachment,
    label: Int,
    enabled: Boolean,
    modifier: Modifier,
    onUpdate: (ComposerAttachment) -> Unit,
) {
    when (label) {
        R.string.attachment_reencode -> {
            AttachmentReencodingOption(file, enabled, modifier, onUpdate)
        }

        R.string.attachment_rename -> {
            AttachmentRenameOption(file, enabled, modifier, onUpdate)
        }

        else -> {
            AttachmentOption(label, attachmentOptionChecked(file, label), enabled, modifier) { value ->
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

private fun attachmentOptionChecked(
    file: ComposerAttachment,
    label: Int,
): Boolean =
    when (label) {
        R.string.attachment_unique_hash -> file.options.uniqueHash
        R.string.attachment_strip_metadata -> file.options.removeMetadata
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
) {
    var settingsOpen by remember(file.id) { mutableStateOf(false) }
    AttachmentOptionTile(
        AttachmentTileState(R.string.attachment_reencode, file.options.reencoding != null, enabled, action = true),
        modifier,
    ) {
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
) {
    var open by remember(file.id) { mutableStateOf(false) }
    val originalBase = file.source.fileName.substringBeforeLast('.', file.source.fileName)
    val renamed =
        file.options.customName != null && file.options.customName != originalBase && !file.options.removeFileName
    AttachmentOptionTile(
        AttachmentTileState(
            R.string.attachment_rename,
            renamed,
            enabled && !file.options.removeFileName,
            action = true,
        ),
        modifier,
    ) { open = true }
    if (open) AttachmentRenameDialog(file, enabled, onUpdate) { open = false }
}
