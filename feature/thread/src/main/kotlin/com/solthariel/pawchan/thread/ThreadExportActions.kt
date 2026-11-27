package com.solthariel.pawchan.thread

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import kotlinx.coroutines.launch
import java.io.IOException

internal data class ThreadExportActions(
    val archive: (ThreadUiState.Content) -> Unit,
    val screenshot: (ThreadUiState.Content) -> Unit,
)

@Composable
internal fun rememberThreadExportActions(
    target: (Uri) -> ThreadExportTarget,
    tools: ThreadToolsState,
): ThreadExportActions {
    val scope = rememberCoroutineScope()
    val writer = remember(scope, tools) { ThreadExportWriter(scope, tools) }
    var html by remember { mutableStateOf<ThreadUiState.Content?>(null) }
    var shot by remember { mutableStateOf<CapturedThreadShot?>(null) }
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
                html = content
                archive.launch("thread-${content.id}.html")
            }
        },
        screenshot = { content ->
            if (!tools.exporting && html == null &&
                shot == null
            ) {
                tools.exporting = true
                scope.launch {
                    try {
                        val positions = content.presentation.postPositions
                        val ordered = tools.selected.filter(positions::containsKey).sortedBy(positions::getValue)
                        shot = tools.shots.freeze(ordered)
                        screenshot.launch("thread-${content.id}-selected.png")
                    } catch (failure: IOException) {
                        tools.notice = failure.message ?: SHOT_MISSING
                    } catch (cancelled: kotlinx.coroutines.CancellationException) {
                        throw cancelled
                    } catch (_: IllegalStateException) {
                        tools.notice = SHOT_MISSING
                    } finally {
                        tools.exporting = false
                    }
                }
            }
        },
    )
}
