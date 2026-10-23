package com.solthariel.pawchan.navigation

import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavType
import androidx.navigation.compose.composable
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

internal fun NavGraphBuilder.appDestinations(
    navController: NavController,
    dependencies: AppDependencies,
    settings: SettingsViewModel,
    bottomActions: AppBottomActions,
    currentBoard: () -> BoardAddress?,
    currentThread: () -> ThreadAddress?,
) {
    statisticsDestination(navController, bottomActions)
    archiveDestination(navController, dependencies)
    boardsDestination(navController, dependencies, bottomActions, currentBoard)
    catalogDestination(navController, dependencies, bottomActions)
    settingsDestination(navController, settings, bottomActions, dependencies.filterStatistics)
    libraryDestination(navController, dependencies, bottomActions, currentThread)
    historyDestinations(navController, dependencies, bottomActions)
    newThreadDestination(navController, dependencies)
    threadDestination(navController, dependencies, bottomActions)
    challengeDestination(navController)
}

private fun NavGraphBuilder.catalogDestination(
    navController: NavController,
    dependencies: AppDependencies,
    bottomActions: AppBottomActions,
) {
    composable(
        route = AppRoute.CATALOG,
        arguments = boardArguments(),
    ) { entry ->
        val board = requireNotNull(entry.arguments?.toBoardAddress())
        val retryToken by
            entry.savedStateHandle
                .getStateFlow(AppRoute.CHALLENGE_RETRY_TOKEN, 0L)
                .collectAsStateWithLifecycle()
        // Выбор другой доски может переиспользовать этот же экран: состояние не должно перетечь.
        key(board) {
            CatalogRoute(
                board = board,
                dependencies = dependencies,
                navigation = catalogNavigation(navController),
                challengeRetryToken = retryToken,
                bottomActions = bottomActions,
            )
        }
    }
}

private fun NavGraphBuilder.boardsDestination(
    navController: NavController,
    dependencies: AppDependencies,
    bottomActions: AppBottomActions,
    currentBoard: () -> BoardAddress?,
) {
    composable(AppRoute.BOARDS) {
        BoardsRoute(
            dependencies = dependencies,
            onBoardSelected = navController::navigateBoard,
            bottomActions = bottomActions,
            currentBoard = currentBoard,
        )
    }
}

private fun NavGraphBuilder.settingsDestination(
    navController: NavController,
    settings: SettingsViewModel,
    bottomActions: AppBottomActions,
    filterStatistics: ContentFilterStatisticsRepository?,
) {
    composable(AppRoute.SETTINGS) {
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
                    navController::navigateCatalogBase,
                    settings::restoreContentFilters,
                    filterStatistics,
                    onStatistics = { navController.navigate(AppRoute.STATISTICS) { launchSingleTop = true } },
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
) {
    composable(AppRoute.LIBRARY) {
        ThreadsRoute(
            dependencies = dependencies,
            navigation =
                ThreadsNavigation(
                    onHistory = { navController.navigate(AppRoute.HISTORY) { launchSingleTop = true } },
                    onCopies = { navController.navigate(AppRoute.COPIES) { launchSingleTop = true } },
                    currentThread = currentThread,
                ),
            bottomActions = bottomActions,
        )
    }
}

private fun NavGraphBuilder.historyDestinations(
    navController: NavController,
    dependencies: AppDependencies,
    bottomActions: AppBottomActions,
) {
    composable(AppRoute.HISTORY) {
        HistoryRoute(
            dependencies = dependencies,
            onBack = navController::popBackStack,
            onThreadClick = { address -> navController.navigateThread(address) },
            bottomActions = bottomActions,
        )
    }
    composable(AppRoute.COPIES) {
        SavedCopiesScreen(onBack = navController::popBackStack, bottomActions = bottomActions)
    }
}

private fun NavGraphBuilder.newThreadDestination(
    navController: NavController,
    dependencies: AppDependencies,
) {
    composable(route = AppRoute.NEW_THREAD, arguments = boardArguments()) { entry ->
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
    composable(
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
    composable(
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

private fun catalogNavigation(navController: NavController): CatalogNavigation =
    CatalogNavigation(
        onThreadClick = navController::navigateThread,
        onLibraryClick = { navController.navigateTab(AppRoute.LIBRARY) },
        onSettingsClick = { navController.navigateTab(AppRoute.SETTINGS) },
        onNewThreadClick = { navController.navigate(AppRoute.newThread(it)) },
        onBoardSelected = navController::navigateBoard,
        onChallenge = { navController.navigate(AppRoute.challenge(it)) },
        onArchive = { navController.navigate(AppRoute.archive(it)) },
    )
