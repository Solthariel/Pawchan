package com.solthariel.pawchan.library

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalResources
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.solthariel.pawchan.PawchanApplication
import com.solthariel.pawchan.media.MediaAttachmentUi
import com.solthariel.pawchan.media.rememberMediaSaveActions
import com.solthariel.pawchan.navigation.chrome.AppBottomActions
import com.solthariel.pawchan.navigation.chrome.AppBottomDestination
import com.solthariel.pawchan.navigation.chrome.AppNavigationWorkspace
import com.solthariel.pawchan.workspace.WorkspaceMediaViewer
import com.solthariel.pawchan.R.string as L

/** «Сохранённые копии» — открываются из «Тредов». */
@Composable
internal fun SavedCopiesScreen(
    onBack: () -> Unit,
    bottomActions: AppBottomActions,
) {
    AppNavigationWorkspace(
        actions = bottomActions,
        selected = AppBottomDestination.THREADS,
        floatingTop = true,
    ) {
        SavedCopiesBody(onBack)
    }
}

@Composable
private fun SavedCopiesBody(onBack: () -> Unit) {
    val res = LocalResources.current

    var query by rememberSaveable { mutableStateOf<String?>(null) }
    var viewer by remember { mutableStateOf<List<MediaAttachmentUi>?>(null) }
    val data = rememberLibraryData()
    val context = LocalContext.current
    val registry = (context.applicationContext as PawchanApplication).zipRegistry
    val indexed by registry.files.collectAsStateWithLifecycle()
    val zipRoutes = remember(indexed) { indexed.filter { it.available }.mapTo(mutableSetOf()) { it.route } }
    LifecycleResumeEffect(registry) {
        registry.refresh()
        onPauseOrDispose { }
    }
    val busy =
        data.binding
            ?.archives
            ?.busy
            ?.collectAsStateWithLifecycle()
            ?.value == true
    com.solthariel.pawchan.core.ui.FloatingTopChrome(toolbar = {
        LibraryToolbar(
            LibraryToolbarState(res.getString(L.ui_saved_copies), data.copies.size, query, busy),
            LibraryToolbarActions(
                onBack = onBack,
                onQuery = { query = it },
                onRefresh = { data.binding?.archives?.updateAll() },
                extraMenu = { SavedCopiesMenu(data.binding?.archives, data.copies.size, busy) },
            ),
        )
    }) {
        SavedCopiesList(data, LibraryView(query.orEmpty(), zipRoutes) { viewer = it })
    }
    data.binding?.let { binding ->
        val save = rememberMediaSaveActions(binding.loaders.mediaDownloadQueue)
        WorkspaceMediaViewer(viewer?.firstOrNull(), viewer.orEmpty(), binding.loaders, save) { viewer = null }
    }
}
