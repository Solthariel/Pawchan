package com.solthariel.pawchan.library

import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.solthariel.pawchan.navigation.AppDependencies
import com.solthariel.pawchan.navigation.AppRoute
import com.solthariel.pawchan.navigation.boardArguments
import com.solthariel.pawchan.navigation.navigateThread
import com.solthariel.pawchan.navigation.saveableKey
import com.solthariel.pawchan.navigation.toBoardAddress

internal fun NavGraphBuilder.archiveDestination(
    navController: NavController,
    dependencies: AppDependencies,
) {
    composable(AppRoute.ARCHIVE, arguments = boardArguments()) { entry ->
        val board = requireNotNull(entry.arguments?.toBoardAddress())
        val repository = dependencies.repositories.boards.details
        val factory = remember(board, repository) { ArchiveViewModel.factory(board, repository) }
        val model: ArchiveViewModel = viewModel(key = "archive:${board.saveableKey}", factory = factory)
        val state by model.state.collectAsStateWithLifecycle()
        ArchiveScreen(
            board,
            state,
            ArchiveActions(
                onBack = navController::popBackStack,
                onRefresh = model::refresh,
                onPrevious = model::previous,
                onNext = model::next,
                onThread = navController::navigateThread,
            ),
        )
    }
}
