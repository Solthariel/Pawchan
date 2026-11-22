package com.solthariel.pawchan.navigation

import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.saveable.SaveableStateHolder
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavType
import androidx.navigation.navArgument
import androidx.navigation.navDeepLink
import com.solthariel.pawchan.catalog.CatalogRoute
import com.solthariel.pawchan.domain.filter.ContentFilterStatisticsRepository
import com.solthariel.pawchan.domain.model.BoardAddress
import com.solthariel.pawchan.domain.model.ThreadAddress
import com.solthariel.pawchan.infrastructure.ChallengeScreen
import com.solthariel.pawchan.library.SavedCopiesScreen
import com.solthariel.pawchan.library.archiveDestination
import com.solthariel.pawchan.navigation.chrome.AppBottomActions
import com.solthariel.pawchan.settings.CacheNotice
import com.solthariel.pawchan.settings.SettingsActions
import com.solthariel.pawchan.settings.SettingsScreen
import com.solthariel.pawchan.settings.SettingsViewModel
import com.solthariel.pawchan.workspace.LocalWorkspace
import com.solthariel.pawchan.workspace.openThread

internal fun NavGraphBuilder.appDestinations(
    navController: NavController,
    dependencies: AppDependencies,
    settings: SettingsViewModel,
    bottomActions: AppBottomActions,
    catalogSession: CatalogSession,
) {
    statisticsDestination(navController, bottomActions)
    archiveDestination(navController, dependencies)
    boardsDestination(navController, dependencies, bottomActions, catalogSession)
    catalogDestination(navController, dependencies, bottomActions, catalogSession.states)
    settingsDestination(navController, settings, bottomActions, dependencies.filterStatistics)
    libraryDestination(
        navController,
        dependencies,
        bottomActions,
        catalogSession.currentThread,
        catalogSession.archiveOwner,
    )
    historyDestinations(navController, dependencies, bottomActions, catalogSession.archiveOwner)
    newThreadDestination(navController, dependencies)
    threadDestination(navController, dependencies, bottomActions)
    challengeDestination(navController)
}

private fun NavGraphBuilder.catalogDestination(
    navController: NavController,
    dependencies: AppDependencies,
    bottomActions: AppBottomActions,
    catalogStates: SaveableStateHolder,
) {
    appComposable(
        route = AppRoute.CATALOG,
        arguments = boardArguments(),
    ) { entry ->
        val board = requireNotNull(entry.arguments?.toBoardAddress())
        val retryToken by
            entry.savedStateHandle
                .getStateFlow(AppRoute.CHALLENGE_RETRY_TOKEN, 0L)
                .collectAsStateWithLifecycle()
        // Выбор другой доски может переиспользовать этот же экран: состояние не должно перетечь.
        catalogStates.SaveableStateProvider(board.saveableKey) {
            key(board) {
                CatalogRoute(
                    board = board,
                    dependencies = dependencies,
                    navigation = catalogNavigation(navController, board),
                    challengeRetryToken = retryToken,
                    bottomActions = bottomActions,
                )
            }
        }
    }
    appComposable(AppRoute.ARCHIVE_BROWSER, arguments = boardArguments()) { entry ->
        com.solthariel.pawchan.navigation.chrome.AppNavigationWorkspace(
            actions = bottomActions,
            selected = com.solthariel.pawchan.navigation.chrome.AppBottomDestination.CATALOG,
            floatingTop = true,
        ) {
            com.solthariel.pawchan.arhivach.ArchiveBrowseScreen(
                dependencies.mediaLoaders,
                onBack = { navController.popBackStack() },
                owner = entry,
            )
        }
    }
}

private fun NavGraphBuilder.boardsDestination(
    navController: NavController,
    dependencies: AppDependencies,
    bottomActions: AppBottomActions,
    catalogSession: CatalogSession,
) {
    appComposable(AppRoute.BOARDS) {
        BoardsRoute(
            dependencies = dependencies,
            onBoardSelected = navController::navigateBoard,
            bottomActions = bottomActions,
            currentBoard = catalogSession.currentBoard,
            owner = catalogSession.archiveOwner,
            boardLists = catalogSession.boardLists,
        )
    }
}

private fun NavGraphBuilder.settingsDestination(
    navController: NavController,
    settings: SettingsViewModel,
    bottomActions: AppBottomActions,
    filterStatistics: ContentFilterStatisticsRepository?,
) {
    appComposable(AppRoute.SETTINGS) {
        val state by settings.state.collectAsStateWithLifecycle()
        val error by settings.writeError.collectAsStateWithLifecycle()
        error?.let { text ->
            CacheNotice(text) { settings.writeError.value = null }
        }
        SettingsScreen(
            state = state,
            actions =
                SettingsActions(
                    settings::setTheme,
                    settings::updateApp,
                    navController::navigateCatalogTab,
                    settings::restoreContentFilters,
                    filterStatistics,
                    onStatistics = { navController.navigate(AppRoute.STATISTICS) { launchSingleTop = true } },
                    onThemeColors = settings::setThemeColors,
                ),
            bottomActions = bottomActions,
        )
    }
}

private fun NavGraphBuilder.libraryDestination(
    navController: NavController,
    dependencies: AppDependencies,
    bottomActions: AppBottomActions,
    currentThread: () -> ThreadAddress?,
    owner: ViewModelStoreOwner,
) {
    appComposable(AppRoute.LIBRARY) {
        ThreadsRoute(
            dependencies = dependencies,
            navigation =
                ThreadsNavigation(
                    onHistory = { navController.navigate(AppRoute.HISTORY) { launchSingleTop = true } },
                    onCopies = { navController.navigate(AppRoute.COPIES) { launchSingleTop = true } },
                    onDownloads = { navController.navigate(AppRoute.DOWNLOADS) { launchSingleTop = true } },
                    currentThread = currentThread,
                    onBack = navController::navigateCatalogTab,
                ),
            bottomActions = bottomActions,
            owner = owner,
        )
    }
}

private fun NavGraphBuilder.historyDestinations(
    navController: NavController,
    dependencies: AppDependencies,
    bottomActions: AppBottomActions,
    owner: ViewModelStoreOwner,
) {
    appComposable(AppRoute.HISTORY) {
        HistoryRoute(
            dependencies = dependencies,
            onBack = navController::popBackStack,
            onThreadClick = { address -> navController.navigateThread(address) },
            bottomActions = bottomActions,
            owner = owner,
        )
    }
    appComposable(AppRoute.DOWNLOADS) {
        val workspace = LocalWorkspace.current
        com.solthariel.pawchan.downloads.DownloadsScreen(
            queue = dependencies.mediaLoaders.mediaDownloadQueue,
            onBack = navController::popBackStack,
            onThread = { address, post ->
                if (workspace != null) workspace.openThread(address, post) else navController.navigateThread(address)
            },
            onArchive = { id -> navController.navigate("download-archive/$id") },
            bottomActions = bottomActions,
            owner = owner,
        )
    }
    appComposable(
        AppRoute.DOWNLOAD_ARCHIVE,
        arguments = listOf(navArgument("archiveId") { type = NavType.LongType }),
    ) { entry ->
        val archiveId = requireNotNull(entry.arguments).getLong("archiveId")
        com.solthariel.pawchan.navigation.chrome.AppNavigationWorkspace(
            actions = bottomActions,
            selected = com.solthariel.pawchan.navigation.chrome.AppBottomDestination.THREADS,
            floatingTop = true,
        ) {
            com.solthariel.pawchan.arhivach.ArchiveBrowseScreen(
                dependencies.mediaLoaders,
                navController::popBackStack,
                entry,
                archiveId,
            )
        }
    }
    appComposable(AppRoute.COPIES) {
        SavedCopiesScreen(onBack = navController::popBackStack, bottomActions = bottomActions)
    }
}

private fun NavGraphBuilder.newThreadDestination(
    navController: NavController,
    dependencies: AppDependencies,
) {
    appComposable(route = AppRoute.NEW_THREAD, arguments = boardArguments()) { entry ->
        val board = requireNotNull(entry.arguments?.toBoardAddress())
        NewThreadRoute(
            board = board,
            dependencies = dependencies,
            onBack = navController::popBackStack,
            onCreated = { address ->
                navController.navigate(AppRoute.thread(address)) {
                    popUpTo(AppRoute.newThread(board)) { inclusive = true }
                }
            },
        )
    }
}

private fun NavGraphBuilder.threadDestination(
    navController: NavController,
    dependencies: AppDependencies,
    bottomActions: AppBottomActions,
) {
    appComposable(
        route = AppRoute.THREAD,
        arguments = threadArguments(),
        deepLinks = listOf(navDeepLink { uriPattern = AppRoute.THREAD_DEEP_LINK }),
    ) { entry ->
        val arguments = requireNotNull(entry.arguments)
        val address = requireNotNull(arguments.toThreadAddress())
        val retryToken by
            entry.savedStateHandle
                .getStateFlow(AppRoute.CHALLENGE_RETRY_TOKEN, 0L)
                .collectAsStateWithLifecycle()
        ThreadRoute(
            address = address,
            dependencies = dependencies,
            onBack = { if (!navController.popBackStack()) navController.navigate(AppRoute.catalog(address.board)) },
            onChallenge = { navController.navigate(AppRoute.challenge(it)) },
            challengeRetryToken = retryToken,
            bottomActions = bottomActions,
        )
    }
}

private fun NavGraphBuilder.challengeDestination(navController: NavController) {
    appComposable(
        route = AppRoute.CHALLENGE,
        arguments =
            listOf(
                navArgument(AppRoute.CHALLENGE_ARGUMENT) {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = null
                },
            ),
    ) { entry ->
        ChallengeScreen(
            url = entry.arguments?.getString(AppRoute.CHALLENGE_ARGUMENT),
            onBack = navController::popBackStack,
            onDone = { navController.completeChallenge() },
        )
    }
}

private fun catalogNavigation(
    navController: NavController,
    board: BoardAddress,
): CatalogNavigation =
    CatalogNavigation(
        onThreadClick = navController::navigateThread,
        onLibraryClick = { navController.navigateTab(AppRoute.LIBRARY) },
        onSettingsClick = { navController.navigateTab(AppRoute.SETTINGS) },
        onNewThreadClick = { navController.navigate(AppRoute.newThread(it)) },
        onBoardSelected = navController::navigateBoard,
        onChallenge = { navController.navigate(AppRoute.challenge(it)) },
        archives =
            CatalogArchiveNavigation(
                openArchive = { navController.navigate(AppRoute.archive(it)) },
                openBrowser = { navController.navigate(AppRoute.archiveBrowser(board)) { launchSingleTop = true } },
            ),
    )
