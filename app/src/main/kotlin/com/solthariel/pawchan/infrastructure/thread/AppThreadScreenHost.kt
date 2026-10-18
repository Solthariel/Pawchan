package com.solthariel.pawchan.infrastructure.thread

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import com.solthariel.pawchan.infrastructure.MediaLoaders
import com.solthariel.pawchan.infrastructure.media.MediaContextMenuHost
import com.solthariel.pawchan.infrastructure.media.MediaMenuTargets
import com.solthariel.pawchan.infrastructure.thread.export.AppThreadExportTarget
import com.solthariel.pawchan.thread.ThreadMediaHost
import com.solthariel.pawchan.thread.ThreadMediaResources
import com.solthariel.pawchan.thread.ThreadScreenHost
import com.solthariel.pawchan.workspace.WorkspaceController

@Composable
internal fun rememberAppThreadScreenHost(
    loaders: MediaLoaders,
    workspace: WorkspaceController?,
    settings: com.solthariel.pawchan.domain.settings.AppSettingsRepository? = null,
): ThreadScreenHost {
    val context = LocalContext.current
    val exportTarget =
        remember(context, loaders.imageLoader) {
            { uri: android.net.Uri -> AppThreadExportTarget(context, loaders.imageLoader, uri) }
        }
    val reading = remember(workspace) { workspace?.let(::AppThreadReadingSession) }
    val preview = rememberAppPostPreviewResources()
    val addFilter =
        com.solthariel.pawchan.settings
            .rememberQuickContentFilter(settings)
    return ThreadScreenHost(
        mediaHost =
            ThreadMediaHost(
                resources =
                    ThreadMediaResources(
                        loaders.thumbnailLoader,
                        loaders.viewer,
                        loaders.mediaDownloadQueue,
                        vocaroo = loaders.vocaroo,
                    ),
                menu = { menu ->
                    MediaContextMenuHost(
                        menu.request,
                        loaders,
                        menu.save,
                        menu.notice,
                        MediaMenuTargets(goToPost = menu.goToPost),
                        menu.close,
                    )
                },
            ),
        reading = reading,
        contentFilterTools = addFilter,
        archive = rememberAppThreadArchiveHost(),
        preview = preview,
        exportTarget = exportTarget,
    )
}
