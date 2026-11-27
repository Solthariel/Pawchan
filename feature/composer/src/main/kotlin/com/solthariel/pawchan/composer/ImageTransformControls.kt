package com.solthariel.pawchan.composer

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.unit.dp
import com.solthariel.pawchan.core.ui.uiChoiceGroup
import com.solthariel.pawchan.domain.drafts.cropToAspect
import com.solthariel.pawchan.feature.composer.R
import com.solthariel.pawchan.feature.composer.R.string as L

@Composable
internal fun ImageAspectRatioControls(
    draft: AttachmentViewerDraft,
    size: ReencodingDimensions,
    enabled: Boolean,
) {
    val res = LocalResources.current

    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        CropPresetGroup(draft, size, enabled, COMPACT_CROP_PRESETS, Modifier.weight(CROP_GROUP_WEIGHT))
        EditorChoiceButton(
            EditorChoice(res.getString(L.ui_change_aspect_ratio_orientation), R.drawable.ic_tool_orientation),
            enabled && draft.cropPreset !in setOf(ImageCropPreset.FREE, ImageCropPreset.SQUARE),
            Modifier.weight(1f),
        ) {
            if (draft.cropPreset ==
                ImageCropPreset.ORIGINAL
            ) {
                draft.cropOriginalInverted = !draft.cropOriginalInverted
            }
            selectCropPreset(draft, size, draft.cropPreset.rotated(), applyAspect = true)
        }
    }
}

@Composable
private fun CropPresetGroup(
    draft: AttachmentViewerDraft,
    size: ReencodingDimensions,
    enabled: Boolean,
    presets: List<ImageCropPreset>,
    modifier: Modifier,
) {
    Row(modifier.uiChoiceGroup().selectableGroup()) {
        presets.forEach { preset ->
            val current = draft.cropPreset
            val selected = current == preset || current == preset.rotated()
            val displayed = if (selected) current else preset
            val icon =
                when (preset) {
                    ImageCropPreset.FREE -> R.drawable.ic_tool_crop_free
                    ImageCropPreset.ORIGINAL -> R.drawable.ic_tool_crop_original
                    else -> null
                }
            EditorChoiceButton(
                EditorChoice(displayed.label, icon, displayed.label, selected),
                enabled,
                Modifier.weight(1f),
            ) {
                draft.cropOriginalInverted = false
                selectCropPreset(draft, size, preset, applyAspect = preset !in MODES)
            }
        }
    }
}

private fun selectCropPreset(
    draft: AttachmentViewerDraft,
    size: ReencodingDimensions,
    preset: ImageCropPreset,
    applyAspect: Boolean,
) {
    val aspect = preset.aspect(draft.editor.edit, size.width, size.height).takeIf { applyAspect }
    aspect?.let {
        val ratio = if (preset == ImageCropPreset.ORIGINAL && draft.cropOriginalInverted) 1f / it else it
        draft.editor.commit(draft.editor.edit.cropToAspect(size.width, size.height, ratio))
    }
    draft.cropPreset = preset
}

@Composable
internal fun ImageRotationControls(
    draft: AttachmentViewerDraft,
    enabled: Boolean,
) {
    val res = LocalResources.current

    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        EditorChoiceButton(
            EditorChoice(res.getString(L.ui_rotate_left_90), R.drawable.ic_tool_rotate_left),
            enabled,
            Modifier.weight(1f),
        ) {
            draft.editor.commit(draft.editor.edit.rotateLeft())
            draft.cropPreset = draft.cropPreset.rotated()
        }
        EditorChoiceButton(
            EditorChoice(res.getString(L.ui_rotate_right_90), R.drawable.ic_tool_rotate_right),
            enabled,
            Modifier.weight(1f),
        ) {
            val turns = (draft.editor.edit.quarterTurns + 1) % QUARTER_TURNS
            draft.editor.commit(draft.editor.edit.copy(quarterTurns = turns))
            draft.cropPreset = draft.cropPreset.rotated()
        }
        ImageMirrorControls(draft, enabled)
    }
}

@Composable
private fun androidx.compose.foundation.layout.RowScope.ImageMirrorControls(
    draft: AttachmentViewerDraft,
    enabled: Boolean,
) {
    val res = LocalResources.current

    EditorChoiceButton(
        EditorChoice(res.getString(L.ui_flip_horizontally), R.drawable.ic_tool_mirror_horizontal),
        enabled,
        Modifier.weight(1f),
    ) {
        draft.editor.commit(draft.editor.edit.flipHorizontal())
    }
    EditorChoiceButton(
        EditorChoice(res.getString(L.ui_flip_vertically), R.drawable.ic_tool_mirror_vertical),
        enabled,
        Modifier.weight(1f),
    ) {
        draft.editor.commit(draft.editor.edit.flipVertical())
    }
}

private const val QUARTER_TURNS = 4
private const val CROP_GROUP_WEIGHT = 5f
private val MODES = setOf(ImageCropPreset.FREE, ImageCropPreset.ORIGINAL)
private val COMPACT_CROP_PRESETS =
    listOf(
        ImageCropPreset.FREE,
        ImageCropPreset.ORIGINAL,
        ImageCropPreset.SQUARE,
        ImageCropPreset.FOUR_THREE,
        ImageCropPreset.SIXTEEN_NINE,
    )
