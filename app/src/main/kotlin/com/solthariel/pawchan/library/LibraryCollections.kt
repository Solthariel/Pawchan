package com.solthariel.pawchan.library

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.solthariel.pawchan.archive.ArchiveCopyUi
import com.solthariel.pawchan.core.ui.FAST_SCROLL_CONTENT_INSET
import com.solthariel.pawchan.core.ui.PawchanFastScrollbar
import com.solthariel.pawchan.core.ui.PostSeparator
import com.solthariel.pawchan.media.MediaAttachmentUi
import com.solthariel.pawchan.media.thumbnail.rememberThumbnailLoading
import com.solthariel.pawchan.navigation.saveableKey
import com.solthariel.pawchan.workspace.LocalWorkspace
import com.solthariel.pawchan.workspace.WorkspaceBinding
import com.solthariel.pawchan.R.string as L

internal data class LibraryData(
    val copies: List<ArchiveCopyUi>,
    val binding: WorkspaceBinding?,
)

@Composable
internal fun rememberLibraryData(): LibraryData {
    val binding = LocalWorkspace.current
    val copies =
        binding
            ?.archives
            ?.display
            ?.collectAsStateWithLifecycle()
            ?.value
            .orEmpty()
    return LibraryData(copies, binding)
}

/** Что показывает список: поиск и просмотр файлов ОП-поста. */
internal data class LibraryView(
    val query: String = "",
    val zipRoutes: Set<String> = emptySet(),
    val openMedia: (List<MediaAttachmentUi>) -> Unit = {},
)

@Composable
internal fun SavedCopiesList(
    data: LibraryData,
    view: LibraryView,
) {
    val res = LocalResources.current

    val binding = data.binding ?: return
    val query = view.query.trim()
    val copies = data.copies.filter { it.card.matches(query) }
    val list = rememberLazyListState()
    val thumbnails = rememberThumbnailLoading(binding.loaders.thumbnailLoader, list)
    val context = LibraryCardContext(thumbnails, view.openMedia)
    Box(Modifier.fillMaxSize()) {
        LazyColumn(
            state = list,
            modifier = Modifier.fillMaxSize(),
            contentPadding =
                PaddingValues(
                    start = 8.dp,
                    top = com.solthariel.pawchan.core.ui.LocalTopOverlayInset.current,
                    end = FAST_SCROLL_CONTENT_INSET,
                    bottom = com.solthariel.pawchan.core.ui.LocalBottomOverlayInset.current + 8.dp,
                ),
        ) {
            if (copies.isEmpty()) {
                item(
                    key = "copies-empty",
                ) { LibraryEmpty(res.getString(L.ui_no_saved_copies_yet)) }
            }
            itemsIndexed(
                copies,
                key = { _, item -> "copy-${item.copy.snapshot.address.saveableKey}" },
                contentType = { _, _ -> "saved-copy" },
            ) { index, item ->
                Column {
                    SavedCopyItem(item, binding, context, view.zipRoutes)
                    PostSeparator(index, copies.lastIndex)
                }
            }
        }
        PawchanFastScrollbar(state = list, modifier = Modifier.align(Alignment.CenterEnd))
    }
}

@Composable
internal fun LibraryEmpty(text: String) {
    Text(text, Modifier.padding(12.dp), color = MaterialTheme.colorScheme.onSurfaceVariant)
}
