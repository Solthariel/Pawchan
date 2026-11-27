package com.solthariel.pawchan.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalResources
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.compose.LifecycleStartEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.solthariel.pawchan.domain.model.DomainOutcome
import com.solthariel.pawchan.domain.model.SiteId
import com.solthariel.pawchan.domain.model.ThreadAddress
import com.solthariel.pawchan.domain.settings.AppSettings
import com.solthariel.pawchan.domain.workspace.WorkspaceState
import com.solthariel.pawchan.domain.workspace.withRepliesDismissed
import com.solthariel.pawchan.library.HistoryScreen
import com.solthariel.pawchan.library.LibraryEntryUi
import com.solthariel.pawchan.library.LibraryUiState
import com.solthariel.pawchan.library.LibraryViewModel
import com.solthariel.pawchan.navigation.chrome.AppBottomActions
import com.solthariel.pawchan.threads.ThreadCounter
import com.solthariel.pawchan.threads.ThreadRowUi
import com.solthariel.pawchan.threads.ThreadSourceUi
import com.solthariel.pawchan.threads.ThreadsLinkActions
import com.solthariel.pawchan.threads.ThreadsPageActions
import com.solthariel.pawchan.threads.ThreadsScreen
import com.solthariel.pawchan.threads.ThreadsScreenActions
import com.solthariel.pawchan.threads.ThreadsScreenInput
import com.solthariel.pawchan.threads.ThreadsSection
import com.solthariel.pawchan.threads.firstUnreadReply
import com.solthariel.pawchan.threads.pageRows
import com.solthariel.pawchan.threads.rememberFavoriteThreadsWatch
import com.solthariel.pawchan.threads.replyRows
import com.solthariel.pawchan.threads.threadTitle
import com.solthariel.pawchan.ui.copyPublicLink
import com.solthariel.pawchan.workspace.LocalWorkspace
import com.solthariel.pawchan.workspace.openThread
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import com.solthariel.pawchan.R.string as L

internal data class ThreadsNavigation(
    val onHistory: () -> Unit,
    val onCopies: () -> Unit,
    val onDownloads: () -> Unit = {},
    val currentThread: () -> ThreadAddress?,
    val onBack: () -> Unit,
)

/** «Треды»: страницы, избранные треды со счётчиками и ответы. */
@Composable
internal fun ThreadsRoute(
    dependencies: AppDependencies,
    navigation: ThreadsNavigation,
    bottomActions: AppBottomActions,
    owner: ViewModelStoreOwner,
) {
    val library = rememberLibraryModel(dependencies, owner)
    val context = LocalContext.current
    LaunchedEffect(context.applicationContext) {
        (context.applicationContext as com.solthariel.pawchan.PawchanApplication)
            .downloadHistory.cachedRecords
            .first { it != com.solthariel.pawchan.downloads.DownloadHistoryState.Loading }
    }
    val libraryState by library.state.collectAsStateWithLifecycle()
    val binding = LocalWorkspace.current
    val registered = dependencies.repositories.boards.sources
    val sources = remember(registered) { registered.associate { it.site to ThreadSourceUi(it.title, it.icon) } }
    val app by (binding?.appSettings?.appSettings ?: flowOf(AppSettings())).collectAsStateWithLifecycle(AppSettings())
    val workspace =
        binding
            ?.controller
            ?.state
            ?.collectAsStateWithLifecycle()
            ?.value ?: WorkspaceState()
    val favorites = (libraryState as? LibraryUiState.Content)?.favorites
    val addresses = remember(favorites) { favorites.orEmpty().map { it.address } }
    val watch =
        rememberFavoriteThreadsWatch(
            binding?.controller?.history,
            dependencies.threadRepository,
            binding?.controller,
            addresses,
            workspace.marks.seen,
        )
    val rows = favoriteThreadRows(favorites, watch.counters, sources)
    val copies =
        binding
            ?.archives
            ?.display
            ?.collectAsStateWithLifecycle()
            ?.value
            ?.size ?: 0
    ThreadsScreen(
        input =
            ThreadsScreenInput(
                pages = remember(workspace.tabs, sources) { pageRows(workspace).withSources(sources) },
                favorites = rows,
                replies =
                    remember(workspace.posts, workspace.replies, workspace.marks, sources) {
                        replyRows(workspace).withSources(sources)
                    },
                current = navigation.currentThread(),
                favoriteAddresses = addresses.toSet(),
                copies = copies,
                refreshing = watch.refreshing,
            ),
        actions = rememberThreadsActions(library, navigation, addresses) { watch.refresh(addresses) },
        bottomActions = bottomActions,
        onBack = navigation.onBack,
        limits = app.behavior.client.reading.navigation,
        onLimitsChange = rememberThreadLimitsUpdate(binding?.appSettings),
    )
}

private fun favoriteThreadRows(
    favorites: List<LibraryEntryUi>?,
    counters: Map<ThreadAddress, ThreadCounter>,
    sources: Map<SiteId, ThreadSourceUi>,
): List<ThreadRowUi>? =
    favorites?.map { entry ->
        ThreadRowUi(
            entry.address,
            threadTitle(entry.address, entry.subject),
            counters[entry.address] ?: ThreadCounter(),
            source = sources[entry.address.board.site] ?: ThreadSourceUi(entry.address.board.site.value),
        )
    }

private fun List<ThreadRowUi>.withSources(sources: Map<SiteId, ThreadSourceUi>): List<ThreadRowUi> =
    map { row -> row.copy(source = sources[row.address.board.site] ?: row.source) }

@Composable
private fun rememberThreadsActions(
    library: LibraryViewModel,
    navigation: ThreadsNavigation,
    favorites: List<ThreadAddress>,
    refreshFavorites: () -> Unit,
): ThreadsScreenActions {
    val res = LocalResources.current

    val mirror = com.solthariel.pawchan.infrastructure.sites.LocalForumMirror.current
    val binding = LocalWorkspace.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val controller = binding?.controller
    return ThreadsScreenActions(
        open = { section, row ->
            val state = controller?.state?.value?.takeIf { section == ThreadsSection.REPLIES }
            val post = state?.let { firstUnreadReply(it, row.address) }
            binding?.openThread(row.address, post)
        },
        pages =
            ThreadsPageActions(
                close = { address -> controller?.closeTab(address) },
                closeAll = { controller?.change { it.copy(tabs = emptyList()) } },
            ),
        setFavorite = { row, favorite ->
            scope.launch {
                if (library.setFavoriteAddress(row.address, row.title, favorite) is DomainOutcome.Failure) {
                    android.widget.Toast
                        .makeText(
                            context,
                            res.getString(L.ui_failed_to_change_favorites),
                            android.widget.Toast.LENGTH_SHORT,
                        ).show()
                }
            }
        },
        dismissReplies = { address -> controller?.change { it.withRepliesDismissed(address, true) } },
        copyLink = { address -> publicThreadLink(address, mirror = mirror)?.let { context.copyPublicLink(it) } },
        refresh = {
            library.refresh()
            controller?.reload()
            if (favorites.isNotEmpty()) refreshFavorites()
        },
        links = ThreadsLinkActions(navigation.onHistory, navigation.onCopies, navigation.onDownloads),
    )
}

@Composable
private fun rememberLibraryModel(
    dependencies: AppDependencies,
    owner: ViewModelStoreOwner,
): LibraryViewModel {
    val factory = remember(dependencies.library) { LibraryViewModel.factory(dependencies.library) }
    val viewModel: LibraryViewModel = viewModel(viewModelStoreOwner = owner, factory = factory)
    LifecycleStartEffect(viewModel) {
        viewModel.refresh()
        onStopOrDispose {}
    }
    return viewModel
}

@Composable
internal fun HistoryRoute(
    dependencies: AppDependencies,
    onBack: () -> Unit,
    onThreadClick: (ThreadAddress) -> Unit,
    bottomActions: AppBottomActions,
    owner: ViewModelStoreOwner,
) {
    val library = rememberLibraryModel(dependencies, owner)
    val state by library.state.collectAsStateWithLifecycle()
    val registered = dependencies.repositories.boards.sources
    val sources = remember(registered) { registered.associate { it.site to ThreadSourceUi(it.title, it.icon) } }
    HistoryScreen(state, onBack, onThreadClick, bottomActions, library::refresh, sources)
}
