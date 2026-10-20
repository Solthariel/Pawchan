package com.solthariel.pawchan.composer

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
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PlainTooltip
import androidx.compose.material3.Text
import androidx.compose.material3.TooltipAnchorPosition
import androidx.compose.material3.TooltipBox
import androidx.compose.material3.TooltipDefaults
import androidx.compose.material3.rememberTooltipState
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
import androidx.compose.ui.unit.sp
import com.solthariel.pawchan.core.ui.uiOptionColors
import com.solthariel.pawchan.feature.composer.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun AttachmentOptionTile(
    state: AttachmentTileState,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    val icon = attachmentTileIcon(state.label)
    val description = stringResource(state.label)
    val status = stringResource(if (state.checked) R.string.composer_option_on else R.string.composer_option_off)
    val tone = uiOptionColors(state.checked, state.enabled)
    val foreground = tone.foreground
    val interactionSource = remember { MutableInteractionSource() }
    // Keep Row parent data on the direct child, outside TooltipBox's internal wrapper.
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        TooltipBox(
            positionProvider = TooltipDefaults.rememberTooltipPositionProvider(TooltipAnchorPosition.Above),
            tooltip = { PlainTooltip { Text(description) } },
            state = rememberTooltipState(),
        ) {
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .attachmentTileInteraction(state, interactionSource, onClick)
                    .semantics {
                        contentDescription = description
                        stateDescription = status
                    },
                contentAlignment = Alignment.Center,
            ) {
                AttachmentOptionIcon(state, icon, foreground, interactionSource)
            }
        }
        Text(
            text = stringResource(attachmentTileCaption(state.label)),
            modifier = Modifier.fillMaxWidth(),
            color = tone.caption,
            style = MaterialTheme.typography.labelSmall,
            autoSize = TextAutoSize.StepBased(minFontSize = 7.sp, maxFontSize = 9.sp, stepSize = 0.5.sp),
            lineHeight = 11.sp,
            letterSpacing = 0.sp,
            textAlign = TextAlign.Center,
            minLines = 2,
            maxLines = 2,
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
                tone.foreground,
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

private fun attachmentTileIcon(label: Int): Int =
    when (label) {
        R.string.attachment_unique_hash -> R.drawable.ic_attachment_option_hash
        R.string.attachment_strip_metadata -> R.drawable.ic_attachment_option_metadata
        R.string.attachment_remove_name -> R.drawable.ic_attachment_option_no_name
        R.string.markup_spoiler -> R.drawable.ic_attachment_option_spoiler
        R.string.attachment_reencode -> R.drawable.ic_attachment_option_reencode
        else -> R.drawable.ic_attachment_option_name
    }

private fun attachmentTileCaption(label: Int): Int =
    when (label) {
        R.string.attachment_unique_hash -> R.string.attachment_hash_short
        R.string.attachment_strip_metadata -> R.string.attachment_metadata_short
        R.string.attachment_remove_name -> R.string.attachment_no_name_short
        R.string.markup_spoiler -> R.string.attachment_spoiler_short
        R.string.attachment_reencode -> R.string.attachment_reencode_short
        else -> R.string.attachment_name_short
    }
