package com.solthariel.pawchan.navigation

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.systemBarsIgnoringVisibility
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.solthariel.pawchan.domain.model.BoardAddress
import com.solthariel.pawchan.infrastructure.thread.AppPostLinkHost
import com.solthariel.pawchan.navigation.chrome.AppBottomActions
import com.solthariel.pawchan.navigation.chrome.AppBottomBar
import com.solthariel.pawchan.navigation.chrome.AppBottomDestination
import com.solthariel.pawchan.navigation.chrome.LocalSharedThreadsBar
import com.solthariel.pawchan.settings.SettingsViewModel
import com.solthariel.pawchan.thread.ThreadPostVoteHost
import com.solthariel.pawchan.workspace.BindWorkspaceReplyChecks
import com.solthariel.pawchan.workspace.BindWorkspaceRoute
import com.solthariel.pawchan.workspace.LocalWorkspace
import com.solthariel.pawchan.workspace.validRestorableRoute
import kotlinx.coroutines.flow.first

// Последний экран читается один раз до того, как стек начнёт перезаписывать его.
@OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
internal fun PawchanNavHost(
    dependencies: AppDependencies,
    settings: SettingsViewModel,
    homeBoard: BoardAddress,
) {
    val workspace = LocalWorkspace.current
    val restored =
        remember {
            initialWorkspaceRestore(
                workspace?.controller,
                settings.loaded.value
                    ?.behavior
                    ?.client
                    ?.reading
                    ?.restoreLastScreen == true,
            )
        }
    val baseRoute =
        rememberSaveable { AppRoute.catalog(restoredBaseBoard(restored.route, homeBoard, restored.base)) }
    val navController = rememberNavController(remember { InstantNavigator() })
    val backStackEntry by navController.currentBackStackEntryAsState()
    val destinationRoute = backStackEntry?.destination?.route
    val currentCatalogRoute =
        backStackEntry
            ?.takeIf { destinationRoute == AppRoute.CATALOG }
            ?.arguments
            ?.toBoardAddress()
            ?.let(AppRoute::catalog)
    val (lastCatalogRoute, selectedForum) = rememberForumSelection(baseRoute, currentCatalogRoute)
    val catalogStates = rememberSaveableStateHolder()
    val archiveOwner = checkNotNull(LocalViewModelStoreOwner.current)
    val boardLists = remember { mutableStateMapOf<String, com.solthariel.pawchan.boards.BoardsListUi>() }
    PrewarmBoardLists(dependencies, archiveOwner, boardLists)
    val lastThreadRoute = rememberLastThreadRoute(navController)
    // «Каталог» всегда возвращает туда, где читали: выбор форума в «Досках» меняет только список.
    val bottomActions = bottomActions(navController) { navController.navigateCatalogTab() }
    BindWorkspaceRoute(navController, workspace?.controller)
    workspace?.let { BindWorkspaceReplyChecks(it) }
    val threadSessions = rememberThreadSessionStores(workspace?.controller)
    CompositionLocalProvider(
        LocalWorkspace provides workspace?.copy(navigate = { address -> navController.navigateThread(address) }),
        LocalThreadSessionStores provides threadSessions,
        LocalSelectedForum provides selectedForum,
        LocalSharedThreadsBar provides (destinationRoute in THREAD_WORKSPACE_ROUTES),
    ) {
        Box(Modifier.fillMaxSize()) {
            AppNavigationContent(
                navController,
                dependencies,
                settings,
                bottomActions,
                rememberCatalogSession(lastCatalogRoute, lastThreadRoute, catalogStates, archiveOwner, boardLists),
                baseRoute,
            )
            if (destinationRoute in THREAD_WORKSPACE_ROUTES) SharedThreadsBar(bottomActions)
        }
    }
    BindTabNavigation(navController, backStackEntry?.id, destinationRoute, restored)
}

@Composable
private fun AppNavigationContent(
    navController: androidx.navigation.NavHostController,
    dependencies: AppDependencies,
    settings: SettingsViewModel,
    bottomActions: AppBottomActions,
    catalogSession: CatalogSession,
    baseRoute: String,
) {
    AppPostLinkHost(onOpenBoard = navController::openLinkedBoard) {
        ThreadPostVoteHost(dependencies.postVotes) {
            AppAnimatedNavHost(navController, baseRoute) {
                appDestinations(navController, dependencies, settings, bottomActions, catalogSession)
            }
        }
    }
}

@OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
private fun androidx.compose.foundation.layout.BoxScope.SharedThreadsBar(actions: AppBottomActions) {
    com.solthariel.pawchan.navigation.chrome.KeyboardAwareBar(
        com.solthariel.pawchan.navigation.chrome
            .keyboardShown(),
        Modifier
            .align(Alignment.BottomCenter)
            .windowInsetsPadding(
                WindowInsets.systemBarsIgnoringVisibility.only(WindowInsetsSides.Horizontal),
            ),
    ) {
        AppBottomBar(actions = actions, selected = AppBottomDestination.THREADS)
    }
}

private fun rememberCatalogSession(
    lastCatalogRoute: State<String>,
    lastThreadRoute: State<String?>,
    catalogStates: androidx.compose.runtime.saveable.SaveableStateHolder,
    archiveOwner: androidx.lifecycle.ViewModelStoreOwner,
    boardLists: MutableMap<String, com.solthariel.pawchan.boards.BoardsListUi>,
) = CatalogSession(
    { AppRoute.catalogBoard(lastCatalogRoute.value) },
    { lastThreadRoute.value?.let(AppRoute::threadAddress) },
    catalogStates,
    archiveOwner,
    boardLists,
)

/**
 * Запуск: основание стека — каталог, последний сохранённый экран открывается поверх (один раз, не при
 * пересоздании активности). «Назад» с корня вкладки сохраняет её стек, как переход на «Каталог».
 */
@Composable
private fun BindTabNavigation(
    navController: androidx.navigation.NavHostController,
    entryId: String?,
    destinationRoute: String?,
    restored: RestoredScreen,
) {
    var restorePending by rememberSaveable { mutableStateOf(true) }
    val incoming = LocalIncomingLinks.current
    val pending = incoming?.pending?.collectAsStateWithLifecycle()?.value
    val workspace = LocalWorkspace.current?.controller
    LaunchedEffect(navController, pending, workspace) {
        if (restorePending) {
            restorePending = false
            if (pending == null) navController.openRestoredRoute(restored.route, restored.reading)
        }
        pending?.let { url ->
            workspace?.ready?.first { it }
            incomingForumLink(url)?.let { navController.openIncomingLink(it, workspace) }
            incoming?.consume(url)
        }
    }
    BackHandler(enabled = destinationRoute in TAB_ROUTES) { navController.navigateCatalogTab() }
    val linkedBoard = remember(entryId) { entryId != null && navController.canReturnFromLinkedBoard() }
    BackHandler(enabled = linkedBoard) { navController.returnFromLinkedBoard() }
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
    onCatalogClick: () -> Unit,
): AppBottomActions =
    AppBottomActions(
        onBoardsClick = { navController.navigateTab(AppRoute.BOARDS) },
        onCatalogClick = onCatalogClick,
        onLibraryClick = { navController.navigateTab(AppRoute.LIBRARY) },
        onSettingsClick = { navController.navigateTab(AppRoute.SETTINGS) },
    )

/** Что открыть при запуске: последний экран, доска, которую читали, и тред до ухода на вкладку. */
private data class RestoredScreen(
    val route: String? = null,
    val base: String? = null,
    val reading: String? = null,
)

private fun initialWorkspaceRestore(
    workspace: com.solthariel.pawchan.workspace.WorkspaceController?,
    restore: Boolean,
): RestoredScreen {
    val state = workspace?.state?.value
    return if (restore && state != null) {
        RestoredScreen(
            state.lastRoute.validRestorableRoute(),
            state.baseRoute?.takeIf { AppRoute.catalogBoard(it) != null },
            state.readingRoute.validRestorableRoute(),
        )
    } else {
        RestoredScreen()
    }
}
