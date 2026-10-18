package com.solthariel.pawchan.infrastructure.media

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.solthariel.pawchan.media.LocalMediaPresentation
import com.solthariel.pawchan.media.MediaPresentation
import com.solthariel.pawchan.workspace.LocalWorkspace

@Composable
internal fun MediaPresentationHost(content: @Composable () -> Unit) {
    val workspace = LocalWorkspace.current
    val files =
        workspace
            ?.archives
            ?.mediaFiles
            ?.collectAsStateWithLifecycle()
            ?.value
            .orEmpty()
    val unread =
        workspace
            ?.controller
            ?.display
            ?.collectAsStateWithLifecycle()
            ?.value
            ?.unread ?: 0
    val presentation = remember(files, unread) { MediaPresentation(files, unread > 0) }
    CompositionLocalProvider(LocalMediaPresentation provides presentation, content = content)
}
