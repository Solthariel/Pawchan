package com.solthariel.pawchan.arhivach

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.solthariel.pawchan.catalog.CatalogAttachmentPicker
import com.solthariel.pawchan.catalog.CatalogBoardLookupBindings
import com.solthariel.pawchan.catalog.CatalogBoardLookupLayer
import com.solthariel.pawchan.core.ui.FAST_SCROLL_CONTENT_INSET
import com.solthariel.pawchan.core.ui.LocalBottomOverlayInset
import com.solthariel.pawchan.core.ui.PageLoadingIndicator
import com.solthariel.pawchan.core.ui.PostSeparator
import com.solthariel.pawchan.data.network.proxy.proxyRouter
import com.solthariel.pawchan.infrastructure.MediaLoaders
import com.solthariel.pawchan.infrastructure.media.MediaContextMenuHost
import com.solthariel.pawchan.media.MediaContentHost
import com.solthariel.pawchan.media.MediaMenuRequest
import com.solthariel.pawchan.media.MediaViewerSelection
import com.solthariel.pawchan.media.rememberMediaSaveActions
import com.solthariel.pawchan.media.thumbnail.rememberThumbnailLoading
import com.solthariel.pawchan.R.string as L

@Composable
internal fun ArchiveBrowseScreen(
    loaders: MediaLoaders,
    onBack: () -> Unit,
    owner: ViewModelStoreOwner,
    openThreadId: Long? = null,
) {
    val factory =
        remember(loaders.contentClient.proxyRouter()) {
            ArchiveBrowseModel.factory(loaders.contentClient.proxyRouter())
        }
    val model: ArchiveBrowseModel = viewModel(viewModelStoreOwner = owner, key = "arhivach-browser", factory = factory)
    LaunchedEffect(openThreadId) { if (openThreadId != null) model.openThread(openThreadId) }
    val state by model.state.collectAsStateWithLifecycle()
    var lookupOpen by rememberSaveable { mutableStateOf(false) }
    var wordSearch by rememberSaveable { mutableStateOf(false) }
    var words by rememberSaveable { mutableStateOf("") }
    val archiveList = rememberArchiveThreadListState(state.feedKey, state.threadCards, words)
    var galleryRequest by rememberSaveable { mutableIntStateOf(0) }
    val closeLookup = {
        lookupOpen = false
        model.queryChanged("")
    }
    val lookup =
        CatalogBoardLookupBindings(
            visible = lookupOpen,
            query = state.search.query,
            onQueryChange = model::queryChanged,
            onOpen = { lookupOpen = true },
            onDismiss = closeLookup,
            suggestions = { ArchiveTagSuggestions(state, model, closeLookup) },
        )
    BackHandler(state.selectedThread != null && !lookupOpen && !wordSearch) {
        if (openThreadId != null) onBack() else model.backToList()
    }
    CatalogBoardLookupLayer(lookup.takeIf { it.visible }) {
        com.solthariel.pawchan.core.ui.FloatingTopChrome(toolbar = {
            ArchiveToolbar(
                state,
                ArchiveToolbarActions(model, onBack, if (openThreadId != null) onBack else null) { galleryRequest++ },
                ArchiveToolbarSearchBindings(
                    lookup,
                    ArchiveWordSearch(wordSearch, words, { wordSearch = it }, { words = it }),
                ),
                galleryEnabled = state.openThread.galleryEnabled,
            )
        }) {
            ArchiveRefreshHost(state.load.loading, model::refresh) {
                ArchiveBrowseFeedback(state, model::refresh) {
                    if (state.selectedThread == null) {
                        ArchiveThreadList(state, model, loaders, words, archiveList, Modifier.fillMaxSize())
                    } else {
                        ArchivePostList(state, loaders, words, galleryRequest, Modifier.fillMaxSize())
                    }
                }
            }
        }
    }
}

@Composable
private fun ArchiveThreadList(
    state: ArchiveBrowseState,
    model: ArchiveBrowseModel,
    loaders: MediaLoaders,
    words: String,
    archiveList: ArchiveThreadListState,
    modifier: Modifier,
) {
    val listState = archiveList.listState
    val thumbnailLoading = rememberThumbnailLoading(loaders.thumbnailLoader, listState)
    val mediaChooser = remember { ArchiveListMediaChooser() }
    var selectedMedia by remember { mutableStateOf<ArchiveListMediaState?>(null) }
    Box(modifier) {
        ArchiveListMediaHost(loaders, mediaChooser, selectedMedia, { selectedMedia = it }) {
            ArchiveThreadRows(
                state = state,
                model = model,
                viewport = ArchiveThreadViewport(archiveList.shown, listState, thumbnailLoading, words.isBlank()),
                onOpenMedia = { mediaState ->
                    mediaChooser.open(mediaState.items) { selectedMedia = it }
                },
            )
        }
    }
}

@Composable
private fun ArchiveListMediaHost(
    loaders: MediaLoaders,
    mediaChooser: ArchiveListMediaChooser,
    selectedMedia: ArchiveListMediaState?,
    onSelectedMediaChange: (ArchiveListMediaState?) -> Unit,
    content: @Composable () -> Unit,
) {
    val mediaSave = rememberMediaSaveActions(loaders.mediaDownloadQueue)
    var mediaMenu by remember { mutableStateOf<MediaMenuRequest?>(null) }
    MediaContentHost(
        selection =
            MediaViewerSelection(
                attachment = selectedMedia?.attachment,
                items = selectedMedia?.items.orEmpty(),
                onSelect = { selected ->
                    selectedMedia?.let { onSelectedMediaChange(it.copy(attachment = selected)) }
                },
            ),
        mediaLoaders = loaders.viewer,
        mediaSave = mediaSave,
        onMenu = { mediaMenu = it },
        onClose = { onSelectedMediaChange(null) },
    ) {
        content()
        if (mediaChooser.visible) {
            CatalogAttachmentPicker(
                attachments = requireNotNull(mediaChooser.choices),
                imageLoader = loaders.imageLoader,
                onDismiss = mediaChooser::dismiss,
                onSelected = { attachment ->
                    mediaChooser.select(attachment) { selected ->
                        onSelectedMediaChange(selected)
                    }
                },
            )
        }
    }
    MediaContextMenuHost(mediaMenu, loaders, mediaSave, onNotice = {}, onClose = { mediaMenu = null })
}

@Composable
private fun ArchiveThreadRows(
    state: ArchiveBrowseState,
    model: ArchiveBrowseModel,
    viewport: ArchiveThreadViewport,
    onOpenMedia: (ArchiveListMediaState) -> Unit,
) {
    val res = LocalResources.current

    val shown = viewport.shown
    val listState = viewport.listState
    val thumbnailLoading = viewport.thumbnailLoading
    val autoLoadPages = viewport.autoLoadPages
    ArchiveAutoLoadEffect(state, viewport, model::loadNextPage)
    LazyColumn(
        state = listState,
        modifier = Modifier.fillMaxSize(),
        contentPadding =
            PaddingValues(
                top = com.solthariel.pawchan.core.ui.LocalTopOverlayInset.current,
                bottom = LocalBottomOverlayInset.current + 16.dp,
            ),
    ) {
        itemsIndexed(
            shown,
            key = { _, thread -> "thread:${thread.summary.id}" },
            contentType = { _, _ -> "archive-thread" },
        ) { index, thread ->
            Column(Modifier.padding(horizontal = FAST_SCROLL_CONTENT_INSET)) {
                ArchiveThreadRow(
                    thread = thread,
                    thumbnailLoading = thumbnailLoading,
                    onClick = { model.openThread(thread.summary.id) },
                    onOpenMedia = { items, attachment ->
                        onOpenMedia(ArchiveListMediaState(attachment, items))
                    },
                )
                PostSeparator(index, shown.lastIndex)
            }
        }
        if (state.list != null && shown.isEmpty() && !state.load.loading) {
            item { Text(res.getString(L.ui_no_threads_found_on_this), Modifier.padding(16.dp)) }
        }
        if (state.list?.hasNext == true) {
            item(key = "archive-load-more") {
                ArchiveLoadMoreFooter(
                    state = state,
                    autoLoadPages = autoLoadPages,
                    onLoadMore = model::loadNextPage,
                )
            }
        }
    }
}

@Composable
internal fun ArchiveLoadMoreFooter(
    state: ArchiveBrowseState,
    autoLoadPages: Boolean,
    onLoadMore: () -> Unit,
) {
    val res = LocalResources.current

    if (autoLoadPages && !state.loadingNextPage && state.nextPageError == null) return
    Box(Modifier.fillMaxWidth().padding(16.dp)) {
        when {
            state.loadingNextPage -> {
                PageLoadingIndicator(
                    description = res.getString(L.ui_loading_next_page),
                    modifier = Modifier.align(Alignment.Center),
                )
            }

            state.nextPageError != null -> {
                TextButton(onClick = onLoadMore) {
                    Text(res.getString(L.ui_retry_e94c, state.nextPageError))
                }
            }

            !autoLoadPages -> {
                TextButton(onClick = onLoadMore) {
                    Text(res.getString(L.ui_load_more))
                }
            }
        }
    }
}

internal data class ArchiveWordSearch(
    val visible: Boolean,
    val query: String,
    val setVisible: (Boolean) -> Unit,
    val setQuery: (String) -> Unit,
)
