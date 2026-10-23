package com.solthariel.pawchan.library

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.solthariel.pawchan.media.MediaAttachmentUi
import com.solthariel.pawchan.media.rememberMediaSaveActions
import com.solthariel.pawchan.navigation.chrome.AppBottomActions
import com.solthariel.pawchan.navigation.chrome.AppBottomDestination
import com.solthariel.pawchan.navigation.chrome.AppNavigationWorkspace
import com.solthariel.pawchan.workspace.WorkspaceMediaViewer

/** «Сохранённые копии» — открываются из «Тредов». */
@Composable
internal fun SavedCopiesScreen(
    onBack: () -> Unit,
    bottomActions: AppBottomActions,
) {
    AppNavigationWorkspace(
        actions = bottomActions,
        selected = AppBottomDestination.THREADS,
    ) {
        SavedCopiesBody(onBack)
    }
}

@Composable
private fun SavedCopiesBody(onBack: () -> Unit) {
    var query by rememberSaveable { mutableStateOf<String?>(null) }
    var viewer by remember { mutableStateOf<List<MediaAttachmentUi>?>(null) }
    val data = rememberLibraryData()
    val busy =
        data.binding
            ?.archives
            ?.busy
            ?.collectAsStateWithLifecycle()
            ?.value == true
    Column(Modifier.fillMaxSize()) {
        LibraryToolbar(
            LibraryToolbarState("Сохранённые копии", data.copies.size, query, busy),
            LibraryToolbarActions(
                onBack = onBack,
                onQuery = { query = it },
                onRefresh = { data.binding?.archives?.updateAll() },
            ),
        )
        SavedCopiesList(data, LibraryView(query.orEmpty()) { viewer = it })
    }
    data.binding?.let { binding ->
        val save = rememberMediaSaveActions(binding.loaders.mediaDownloadQueue)
        WorkspaceMediaViewer(viewer?.firstOrNull(), viewer.orEmpty(), binding.loaders, save) { viewer = null }
    }
}
