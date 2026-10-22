package com.solthariel.pawchan

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import com.solthariel.pawchan.data.database.RoomStorage
import com.solthariel.pawchan.data.drafts.FileWorkspaceRepository
import com.solthariel.pawchan.infrastructure.AppNetworkStack
import com.solthariel.pawchan.workspace.LocalWorkspace
import com.solthariel.pawchan.workspace.WorkspaceBinding
import com.solthariel.pawchan.workspace.WorkspaceController

@Composable
internal fun PreviewTestWorkspace(content: @Composable () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val network =
        remember(context) {
            AppNetworkStack.create(context, (context.applicationContext as PawchanApplication).preferences)
        }
    val storage = remember(context) { RoomStorage.create(context) }
    val directory =
        remember(context) {
            context.cacheDir.resolve("preview-workspace-${System.nanoTime()}").apply { check(mkdirs()) }
        }
    val controller = remember { WorkspaceController(scope, FileWorkspaceRepository(directory)) }
    DisposableEffect(network, storage) {
        onDispose {
            network.close()
            storage.close()
            directory.deleteRecursively()
        }
    }
    CompositionLocalProvider(
        com.solthariel.pawchan.infrastructure.LocalGithubNetwork provides network.github,
        LocalWorkspace provides WorkspaceBinding(controller, network.mediaLoaders, {}, storage.library),
    ) {
        content()
    }
}
