package com.solthariel.pawchan.media

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.solthariel.pawchan.media.video.BindVideoPipReturn
import com.solthariel.pawchan.media.video.VideoSessionPool

internal data class ViewerSession(
    val navigation: MediaViewerNavigation,
    val autoPlay: Boolean,
)

/** Playback navigation belongs to the viewer lifetime, independently of the caller's gallery selection. */
@Composable
internal fun rememberViewerSession(selection: MediaViewerSelection): ViewerSession {
    var afterScroll by remember { mutableStateOf(false) }
    val preferences = LocalMediaPreferences.current
    val attachment = selection.attachment
    val viewing = attachment != null && attachment.viewer != AttachmentViewer.NONE
    val events = LocalMediaViewEvents.current
    val seen = remember { mutableSetOf<String>() }
    LaunchedEffect(attachment, viewing) {
        if (viewing && seen.add(attachment.originalUrl)) events.opened(attachment)
        if (!viewing) seen.clear()
    }
    LaunchedEffect(viewing) {
        if (!viewing) {
            afterScroll = false
            VideoSessionPool.clear()
        }
    }
    DisposableEffect(Unit) { onDispose { VideoSessionPool.clear() } }
    BindVideoPipReturn(selection.items, selection.onSelect)
    val navigation =
        selection
            .copy(onSelect = {
                afterScroll = true
                selection.onSelect(it)
            })
            .navigation()
    return ViewerSession(navigation, !afterScroll || preferences.playAfterScroll)
}
