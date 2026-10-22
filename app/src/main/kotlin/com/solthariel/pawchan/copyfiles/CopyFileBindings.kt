package com.solthariel.pawchan.copyfiles

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.solthariel.pawchan.PawchanApplication
import com.solthariel.pawchan.workspace.LocalWorkspace

/** App-owned SAF resources, shared only by actual copy-file consumers. No I/O in composition. */
@Composable
internal fun rememberCopyFiles(kind: CopyFileKind): CopyFileActions {
    val context = LocalContext.current
    val settings = LocalWorkspace.current?.appSettings
    val loaded = settings?.appSettings?.collectAsStateWithLifecycle(initialValue = null)?.value
    val tree =
        loaded
            ?.behavior
            ?.client
            ?.downloads
            ?.treeUri
    val scope = rememberCoroutineScope()
    val app = context.applicationContext as PawchanApplication
    return remember(context.contentResolver, tree, kind, scope, loaded != null) {
        CopyFileActions(
            app.copyDirectoryStore,
            tree,
            kind,
            scope,
            ready = settings == null || loaded != null,
        )
    }
}
