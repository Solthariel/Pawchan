package com.solthariel.pawchan.composer

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.window.DialogProperties
import coil3.ImageLoader
import coil3.compose.AsyncImage
import com.solthariel.pawchan.core.ui.FullScreenModalWindow
import com.solthariel.pawchan.core.ui.KeepWindowBounds
import com.solthariel.pawchan.core.ui.focusedWindowImePadding
import com.solthariel.pawchan.media.LocalVideoAttachmentPreview

@Composable
internal fun ComposerAttachmentViewer(
    file: ComposerAttachment,
    imageLoader: ImageLoader,
    enabled: Boolean,
    onDismiss: () -> Unit,
    onUpdate: (ComposerAttachment) -> Unit,
    onRemove: () -> Unit,
) {
    PreparedAttachmentViewer(file, onDismiss) {
        ComposerAttachmentViewerWindow(file, imageLoader, enabled, onDismiss, onUpdate, onRemove)
    }
}

@Composable
private fun ComposerAttachmentViewerWindow(
    file: ComposerAttachment,
    imageLoader: ImageLoader,
    enabled: Boolean,
    onDismiss: () -> Unit,
    onUpdate: (ComposerAttachment) -> Unit,
    onRemove: () -> Unit,
) {
    val draft = remember(file.id) { AttachmentViewerDraft(file.options) }
    val scope = rememberCoroutineScope()
    val saving = remember(file.id, scope) { AttachmentSaveState(scope, kotlinx.coroutines.Dispatchers.Default) }
    val pending = file.copy(options = draft.currentOptions)
    val close = {
        if (enabled && !saving.busy) {
            if (draft.editor.inGesture) draft.editor.finish()
            val current = file.copy(options = draft.currentOptions)
            if (!current.validName) draft.selectTab(AttachmentEditorTab.NAME)
            if (draft.changed || !current.validName) {
                saving.save(current) { prepared ->
                    onUpdate(prepared)
                    onDismiss()
                }
            } else {
                onDismiss()
            }
        }
    }
    FullScreenModalWindow(
        onDismissRequest = close,
        properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false),
        dimUnderlying = false,
    ) {
        AttachmentViewerWindow()
        KeepWindowBounds()
        Column(
            Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .systemBarsPadding()
                .focusedWindowImePadding(),
        ) {
            AttachmentViewerHeader(enabled && !saving.busy, close, onRemove, draft)
            Box(Modifier.weight(1f)) {
                AttachmentEditorContent(pending, imageLoader, draft, enabled && !saving.busy)
                AttachmentSaveStatus(saving)
            }
        }
    }
}

@Composable
internal fun ComposerViewerMedia(
    file: ComposerAttachment,
    imageLoader: ImageLoader,
) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        val edit = file.options.edit
        if (edit != null) {
            ComposerEditedPreview(file.source.content, edit, file.displayName, Modifier.fillMaxSize())
        } else if (file.video) {
            LocalVideoAttachmentPreview(file.id, file.displayName, file.source.content, file.dimensions)
        } else {
            AsyncImage(
                model = file.source.content,
                imageLoader = imageLoader,
                contentDescription = file.displayName,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Fit,
            )
        }
    }
}
