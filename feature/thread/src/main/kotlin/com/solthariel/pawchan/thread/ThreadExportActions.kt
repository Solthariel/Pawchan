package com.solthariel.pawchan.thread

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.toArgb
import com.solthariel.pawchan.core.ui.LocalPostDisplayPreferences
import com.solthariel.pawchan.core.ui.PostDisplayPreferences

internal data class ThreadExportActions(
    val archive: (ThreadUiState.Content) -> Unit,
    val screenshot: (ThreadUiState.Content) -> Unit,
)

internal data class ThreadExportRequest(
    val content: ThreadUiState.Content,
    val selected: Set<Long> = emptySet(),
    val shotStyle: ThreadShotStyle = ThreadShotStyle(),
    val displayPreferences: PostDisplayPreferences = PostDisplayPreferences(),
)

@Composable
internal fun rememberThreadExportActions(
    target: (Uri) -> ThreadExportTarget,
    tools: ThreadToolsState,
    appearance: ThreadAppearance,
): ThreadExportActions {
    val scope = rememberCoroutineScope()
    val writer = remember(scope, tools) { ThreadExportWriter(scope, tools) }
    val displayPreferences = LocalPostDisplayPreferences.current
    val colors = MaterialTheme.colorScheme
    val shotStyle =
        ThreadShotStyle(
            textSizeSp = appearance.textSize,
            background = colors.background.toArgb(),
            surface = colors.surface.toArgb(),
            border = colors.outlineVariant.toArgb(),
            text = colors.onSurface.toArgb(),
            secondaryText = colors.onSurfaceVariant.toArgb(),
            accent = LINK_COLOR.toArgb(),
        )
    var html by remember { mutableStateOf<ThreadExportRequest?>(null) }
    var shot by remember { mutableStateOf<ThreadExportRequest?>(null) }
    val archive =
        rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("text/html")) { uri ->
            val request = html
            html = null
            if (uri != null && request != null) writer.saveHtml(request, target(uri))
        }
    val screenshot =
        rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("image/png")) { uri ->
            val request = shot
            shot = null
            if (uri != null && request != null) writer.saveShot(request, target(uri))
        }
    return ThreadExportActions(
        archive = { content ->
            if (!tools.exporting && html == null &&
                shot == null
            ) {
                html = ThreadExportRequest(content)
                archive.launch("thread-${content.id}.html")
            }
        },
        screenshot = { content ->
            if (!tools.exporting && html == null &&
                shot == null
            ) {
                shot = ThreadExportRequest(content, tools.selected.toSet(), shotStyle, displayPreferences)
                screenshot.launch("thread-${content.id}-selected.png")
            }
        },
    )
}
