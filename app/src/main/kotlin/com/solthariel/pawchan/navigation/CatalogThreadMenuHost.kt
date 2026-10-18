package com.solthariel.pawchan.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import com.solthariel.pawchan.catalog.CatalogThreadMenuHost
import com.solthariel.pawchan.domain.library.ThreadLibraryRepository
import com.solthariel.pawchan.domain.model.DomainOutcome
import com.solthariel.pawchan.domain.workspace.withHiddenThread
import com.solthariel.pawchan.infrastructure.sites.LocalForumMirror
import com.solthariel.pawchan.infrastructure.sites.appForumWebLinks
import com.solthariel.pawchan.infrastructure.thread.AppPostMenuTransfer
import com.solthariel.pawchan.workspace.LocalWorkspace
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@Composable
internal fun rememberCatalogThreadMenuHost(library: ThreadLibraryRepository): CatalogThreadMenuHost? {
    val mirror = LocalForumMirror.current
    val workspace = LocalWorkspace.current?.controller ?: return null
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    return remember(workspace, scope, context, library, mirror) {
        val sites = appForumWebLinks(mirror).associateBy { it.site }
        val transfer = AppPostMenuTransfer(context)
        CatalogThreadMenuHost(
            hiddenThreads =
                workspace.state
                    .map { it.marks.hidden }
                    .stateIn(
                        scope,
                        SharingStarted.WhileSubscribed(HIDDEN_SUBSCRIPTION_TIMEOUT_MILLIS),
                        workspace.state.value.marks.hidden,
                    ),
            setHidden = { address, hide ->
                workspace.change { it.withHiddenThread(address, hide) }
            },
            threadLink = { address -> sites[address.board.site]?.threadUrl(address) },
            copyLink = transfer::copy,
            shareLink = transfer::share,
            isFavorite = { address ->
                (library.loadEntry(address) as? DomainOutcome.Success)?.value?.favorite == true
            },
            setFavorite = { address, subject, favorite ->
                scope.launch { library.setFavorite(address, subject, favorite, System.currentTimeMillis()) }
            },
        )
    }
}

private const val HIDDEN_SUBSCRIPTION_TIMEOUT_MILLIS = 5_000L
