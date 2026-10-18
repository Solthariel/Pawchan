package com.solthariel.pawchan.archive

import androidx.compose.runtime.Composable
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.solthariel.pawchan.workspace.LocalWorkspace

@Composable
internal fun archivedMediaUrl(url: String): String {
    val files =
        LocalWorkspace.current
            ?.archives
            ?.mediaFiles
            ?.collectAsStateWithLifecycle()
            ?.value
    return files?.get(url)?.takeIf { it.isNotEmpty() } ?: url
}
