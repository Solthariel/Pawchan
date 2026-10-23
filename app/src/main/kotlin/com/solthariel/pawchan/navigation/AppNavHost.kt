package com.solthariel.pawchan.navigation

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.solthariel.pawchan.domain.boards.BoardDirectoryRepository
import com.solthariel.pawchan.domain.boards.BoardPreferencesRepository
import com.solthariel.pawchan.domain.boards.PostingAccessRepository
import com.solthariel.pawchan.domain.catalog.CatalogRepository
import com.solthariel.pawchan.domain.library.ThreadLibraryRepository
import com.solthariel.pawchan.domain.model.BoardAddress
import com.solthariel.pawchan.domain.model.ThreadAddress
import com.solthariel.pawchan.domain.posting.PostingRepository
import com.solthariel.pawchan.domain.postpreferences.PostPreferencesRepository
import com.solthariel.pawchan.domain.thread.ThreadRepository
import com.solthariel.pawchan.infrastructure.thread.AppPostLinkHost
import com.solthariel.pawchan.navigation.chrome.AppBottomActions
import com.solthariel.pawchan.settings.SettingsViewModel
import com.solthariel.pawchan.thread.ThreadPostVoteHost
import com.solthariel.pawchan.workspace.BindWorkspaceReplyChecks
import com.solthariel.pawchan.workspace.BindWorkspaceRoute
import com.solthariel.pawchan.workspace.LocalWorkspace
import com.solthariel.pawchan.workspace.validRestorableRoute
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

@Composable
internal fun PawchanNavHost(
    dependencies: AppDependencies,
    settings: SettingsViewModel,
    homeBoard: BoardAddress,
) {
    val workspace = LocalWorkspace.current
    // Последний экран читается один раз до того, как стек начнёт перезаписывать его.
    val restoredRoute =
        remember {
            initialWorkspaceRoute(
                workspace?.controller,
                settings.loaded.value
                    ?.behavior
                    ?.client
                    ?.reading
                    ?.restoreLastScreen == true,
            )
        }
    val baseRoute = rememberSaveable { AppRoute.catalog(restoredBaseBoard(restoredRoute, homeBoard)) }
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    var lastCatalogRoute by rememberSaveable { mutableStateOf(baseRoute) }
    val destinationRoute = backStackEntry?.destination?.route
    val currentCatalogRoute =
        backStackEntry
            ?.takeIf { destinationRoute == AppRoute.CATALOG }
            ?.arguments
            ?.toBoardAddress()
            ?.let(AppRoute::catalog)
    LaunchedEffect(currentCatalogRoute) {
        currentCatalogRoute?.let { route -> lastCatalogRoute = route }
    }
    val lastThreadRoute = rememberLastThreadRoute(navController)
    val motion = remember { AppNavigationMotion() }
    val bottomActions = bottomActions(navController, motion)
    BindWorkspaceRoute(navController, workspace?.controller)
    workspace?.let { BindWorkspaceReplyChecks(it) }
    val threadSessions = rememberThreadSessionStores(workspace?.controller)
    CompositionLocalProvider(
        LocalWorkspace provides workspace?.copy(navigate = { address -> navController.navigateThread(address) }),
        LocalThreadSessionStores provides threadSessions,
    ) {
        AppPostLinkHost(onOpenBoard = navController::navigateBoard) {
            ThreadPostVoteHost(dependencies.postVotes) {
                AppAnimatedNavHost(navController, baseRoute, motion) {
                    appDestinations(
                        navController,
                        dependencies,
                        settings,
                        bottomActions,
                        { AppRoute.catalogBoard(lastCatalogRoute) },
                        { lastThreadRoute.value?.let(AppRoute::threadAddress) },
                    )
                }
            }
        }
    }
    BindTabNavigation(navController, destinationRoute, restoredRoute)
}

/**
 * Запуск: основание стека — каталог, последний сохранённый экран открывается поверх (один раз, не при
 * пересоздании активности). «Назад» с корня вкладки сохраняет её стек, как переход на «Каталог».
 */
@Composable
private fun BindTabNavigation(
    navController: androidx.navigation.NavHostController,
    destinationRoute: String?,
    restoredRoute: String?,
) {
    var restorePending by rememberSaveable { mutableStateOf(true) }
    val incoming = LocalIncomingLinks.current
    val pending = incoming?.pending?.collectAsStateWithLifecycle()?.value
    val workspace = LocalWorkspace.current?.controller
    LaunchedEffect(navController, pending, workspace) {
        if (restorePending) {
            restorePending = false
            if (pending == null) navController.openRestoredRoute(restoredRoute)
        }
        pending?.let { url ->
            workspace?.ready?.first { it }
            incomingForumLink(url)?.let { navController.openIncomingLink(it, workspace) }
            incoming?.consume(url)
        }
    }
    BackHandler(enabled = destinationRoute in TAB_ROUTES) { navController.navigateCatalogBase() }
}

/** Последний открытый тред — подсвечивается в «Страницах». */
@Composable
private fun rememberLastThreadRoute(navController: androidx.navigation.NavHostController): State<String?> {
    val backStackEntry by navController.currentBackStackEntryAsState()
    val lastThreadRoute = rememberSaveable { mutableStateOf<String?>(null) }
    val currentThreadRoute =
        backStackEntry
            ?.takeIf { it.destination.route == AppRoute.THREAD }
            ?.arguments
            ?.toThreadAddress()
            ?.let(AppRoute::thread)
    LaunchedEffect(currentThreadRoute) {
        currentThreadRoute?.let { route -> lastThreadRoute.value = route }
    }
    return lastThreadRoute
}

private fun bottomActions(
    navController: androidx.navigation.NavHostController,
    motion: AppNavigationMotion,
): AppBottomActions =
    AppBottomActions(
        onBoardsClick = { motion.tab(navController) { navController.navigateTab(AppRoute.BOARDS) } },
        onCatalogClick = { motion.tab(navController) { navController.navigateCatalogBase() } },
        onLibraryClick = { motion.tab(navController) { navController.navigateTab(AppRoute.LIBRARY) } },
        onSettingsClick = { motion.tab(navController) { navController.navigateTab(AppRoute.SETTINGS) } },
    )

private fun initialWorkspaceRoute(
    workspace: com.solthariel.pawchan.workspace.WorkspaceController?,
    restore: Boolean,
): String? =
    if (restore) {
        workspace
            ?.state
            ?.value
            ?.lastRoute
            .validRestorableRoute()
    } else {
        null
    }
