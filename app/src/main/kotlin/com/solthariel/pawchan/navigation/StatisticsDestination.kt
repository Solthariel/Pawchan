package com.solthariel.pawchan.navigation

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.solthariel.pawchan.data.preferences.DataStoreSettingsRepository
import com.solthariel.pawchan.navigation.chrome.AppBottomActions
import com.solthariel.pawchan.settings.UsageStatisticsScreen
import com.solthariel.pawchan.workspace.LocalWorkspace

internal fun NavGraphBuilder.statisticsDestination(
    navController: NavController,
    bottomActions: AppBottomActions,
) {
    composable(AppRoute.STATISTICS) {
        val settings = LocalWorkspace.current?.appSettings as? DataStoreSettingsRepository
        UsageStatisticsScreen(settings?.usage, navController::popBackStack, bottomActions)
    }
}
