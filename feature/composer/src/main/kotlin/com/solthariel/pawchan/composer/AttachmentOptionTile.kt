package com.solthariel.pawchan.composer

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.solthariel.pawchan.core.ui.uiOptionColors
import com.solthariel.pawchan.feature.composer.R

@Composable
internal fun AttachmentOptionTile(
    state: AttachmentTileState,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    val icon = attachmentTileIcon(state.label, state.checked)
    val description = stringResource(state.label)
    val status = stringResource(if (state.checked) R.string.composer_option_on else R.string.composer_option_off)
    val tone = uiOptionColors(state.checked, state.enabled)
    val foreground by animateColorAsState(
        tone.foreground,
        tween(TILE_TRANSITION_MS),
        label = "attachment-control-color",
    )
    val interactionSource = remember { MutableInteractionSource() }
    // Apply the caller's weight to the direct Column child of the row.
    Column(
        modifier
            .clip(RoundedCornerShape(12.dp))
            .attachmentTileInteraction(state, interactionSource, onClick)
            .semantics(mergeDescendants = true) {
                contentDescription = description
                if (!state.action || state.checked || state.label == R.string.attachment_rename) {
                    stateDescription = status
                }
            },
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            Modifier
                .fillMaxWidth()
                .height(EDITOR_CONTROL_HEIGHT),
            contentAlignment = Alignment.Center,
        ) {
            AttachmentOptionIcon(state, icon, foreground, interactionSource)
        }
        Text(
            text = stringResource(attachmentTileCaption(state.label)),
            modifier = Modifier.fillMaxWidth(),
            color = LocalContentColor.current.copy(alpha = if (state.enabled) 1f else 0.55f),
            style = MaterialTheme.typography.labelSmall,
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun AttachmentOptionIcon(
    state: AttachmentTileState,
    icon: Int,
    foreground: Color,
    interactionSource: MutableInteractionSource,
) {
    val tone = uiOptionColors(state.checked, state.enabled)
    val pressed by interactionSource.collectIsPressedAsState()
    val focused by interactionSource.collectIsFocusedAsState()
    Box(
        Modifier
            .size(38.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(tone.background)
            .border(
                1.dp,
                foreground,
                RoundedCornerShape(10.dp),
            )
            // Keep press/focus feedback inside the fixed tile, not the wider touch target.
            .drawWithContent {
                drawContent()
                if (pressed || focused) drawRect(foreground.copy(alpha = 0.12f))
            },
        contentAlignment = Alignment.Center,
    ) {
        Icon(painterResource(icon), contentDescription = null, tint = foreground, modifier = Modifier.size(20.dp))
    }
}

private fun Modifier.attachmentTileInteraction(
    state: AttachmentTileState,
    interactionSource: MutableInteractionSource,
    onClick: () -> Unit,
): Modifier =
    if (state.action) {
        clickable(
            interactionSource = interactionSource,
            indication = null,
            enabled = state.enabled,
            role = Role.Button,
            onClick = onClick,
        )
    } else {
        toggleable(
            value = state.checked,
            interactionSource = interactionSource,
            indication = null,
            enabled = state.enabled,
            role = Role.Checkbox,
        ) { onClick() }
    }

internal fun attachmentTileIcon(
    label: Int,
    checked: Boolean,
): Int =
    when (label) {
        R.string.attachment_unique_hash -> {
            if (checked) R.drawable.ic_attachment_option_hash_off else R.drawable.ic_attachment_option_hash
        }

        R.string.attachment_strip_metadata -> {
            if (checked) R.drawable.ic_attachment_option_metadata_off else R.drawable.ic_attachment_option_metadata
        }

        R.string.attachment_remove_name -> {
            if (checked) R.drawable.ic_attachment_option_no_name_off else R.drawable.ic_attachment_option_no_name
        }

        R.string.markup_spoiler -> {
            if (checked) R.drawable.ic_attachment_option_spoiler_off else R.drawable.ic_attachment_option_spoiler
        }

        R.string.attachment_reencode -> {
            R.drawable.ic_attachment_option_reencode
        }

        R.string.attachment_edit_image -> {
            R.drawable.ic_attachment_edit_image
        }

        else -> {
            EDITOR_TILE_ICONS[label] ?: R.drawable.ic_attachment_option_name
        }
    }

private fun attachmentTileCaption(label: Int): Int =
    when (label) {
        R.string.attachment_unique_hash -> R.string.attachment_hash_short
        R.string.attachment_strip_metadata -> R.string.attachment_metadata_short
        R.string.attachment_remove_name -> R.string.attachment_no_name_short
        R.string.markup_spoiler -> R.string.attachment_spoiler_short
        R.string.attachment_reencode -> R.string.attachment_reencode_short
        R.string.attachment_edit_image -> R.string.attachment_edit_image
        R.string.attachment_rename -> R.string.attachment_name_short
        R.string.editor_back -> R.string.editor_back_short
        else -> label
    }

private const val TILE_TRANSITION_MS = 160

private val EDITOR_TILE_ICONS =
    mapOf(
        R.string.editor_back to R.drawable.ic_editor_back,
        R.string.editor_crop to R.drawable.ic_editor_crop,
        R.string.editor_rotate to R.drawable.ic_editor_rotate,
        R.string.editor_flip to R.drawable.ic_editor_flip,
        R.string.editor_draw to R.drawable.ic_editor_draw,
        R.string.editor_redact to R.drawable.ic_editor_redact,
        R.string.editor_compare to R.drawable.ic_editor_compare,
    )
