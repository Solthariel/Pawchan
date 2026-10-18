package com.solthariel.pawchan.workspace

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.solthariel.pawchan.infrastructure.MediaLoaders
import com.solthariel.pawchan.media.MediaAttachmentUi
import com.solthariel.pawchan.media.MediaContentHost
import com.solthariel.pawchan.media.MediaSaveActions
import com.solthariel.pawchan.media.MediaViewerSelection

@Composable
internal fun WorkspaceMediaViewer(
    attachment: MediaAttachmentUi?,
    group: List<MediaAttachmentUi>,
    loaders: MediaLoaders,
    save: MediaSaveActions,
    onClose: () -> Unit,
) {
    if (attachment == null) return
    var current by remember(attachment) { mutableStateOf(attachment) }
    MediaContentHost(MediaViewerSelection(current, group) { current = it }, loaders.viewer, save, null, onClose) {}
}
