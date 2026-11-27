package com.solthariel.pawchan.composer

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.unit.dp
import coil3.ImageLoader
import com.solthariel.pawchan.core.ui.TransientNotice
import com.solthariel.pawchan.feature.composer.R.string as L

@Composable
internal fun AttachmentEditorContent(
    file: ComposerAttachment,
    imageLoader: ImageLoader,
    draft: AttachmentViewerDraft,
    enabled: Boolean,
) {
    val res = LocalResources.current

    val dimensions = rememberAttachmentEditorDimensions(file)
    var limitReached by remember(file.id) { mutableStateOf(false) }
    val actions = AttachmentEditorActions(enabled) { limitReached = true }
    AttachmentEditorLayout(file, imageLoader, draft, dimensions, actions)
    TransientNotice(
        if (limitReached) res.getString(L.ui_edit_or_stroke_length_limit) else null,
        onDismiss = { limitReached = false },
    )
}

@Composable
private fun AttachmentEditorLayout(
    file: ComposerAttachment,
    imageLoader: ImageLoader,
    draft: AttachmentViewerDraft,
    dimensions: ReencodingDimensions?,
    actions: AttachmentEditorActions,
) {
    BoxWithConstraints(Modifier.fillMaxSize()) {
        val preferredHeight = EDITOR_PANEL_HEIGHT.dp * LocalDensity.current.fontScale
        val panelHeight = minOf(preferredHeight, maxHeight * EDITOR_PANEL_FRACTION)
        Column(Modifier.fillMaxSize()) {
            Box(Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                AttachmentEditorCanvas(file, imageLoader, draft, dimensions, actions, Modifier.fillMaxSize())
            }
            AttachmentEditorPanel(file, draft, dimensions, actions.enabled, Modifier.height(panelHeight))
        }
    }
}

private const val EDITOR_PANEL_HEIGHT = 184
private const val EDITOR_PANEL_FRACTION = .55f
