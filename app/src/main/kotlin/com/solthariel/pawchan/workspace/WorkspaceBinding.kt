package com.solthariel.pawchan.workspace

import androidx.compose.runtime.staticCompositionLocalOf
import com.solthariel.pawchan.domain.model.ThreadAddress
import com.solthariel.pawchan.infrastructure.MediaLoaders

/** UI bindings are provided by the navigation composition root, not a process-wide service locator. */
internal data class WorkspaceBinding(
    val controller: WorkspaceController,
    val loaders: MediaLoaders,
    val navigate: (ThreadAddress) -> Unit,
    val library: com.solthariel.pawchan.domain.library.ThreadLibraryRepository,
    val archives: com.solthariel.pawchan.archive.ArchiveController? = null,
    val appSettings: com.solthariel.pawchan.domain.settings.AppSettingsRepository? = null,
)

/** Открыть тред из ссылки/адреса; [post] — прокрутить к посту, null — к позиции чтения. */
internal fun WorkspaceBinding.openThread(
    address: ThreadAddress,
    post: Long?,
) {
    controller.offlineAddress = null
    controller.requestedPost = post
    navigate(address)
}

internal val LocalWorkspace = staticCompositionLocalOf<WorkspaceBinding?> { null }
