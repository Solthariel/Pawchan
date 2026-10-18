package com.solthariel.pawchan.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import com.solthariel.pawchan.core.ui.uiScreenEnter
import com.solthariel.pawchan.core.ui.uiScreenExit

@Composable
internal fun AppAnimatedNavHost(
    controller: NavHostController,
    startRoute: String,
    motion: AppNavigationMotion,
    builder: NavGraphBuilder.() -> Unit,
) {
    NavHost(
        navController = controller,
        startDestination = startRoute,
        enterTransition = { uiScreenEnter(tab = motion.isTab(initialState, targetState)) },
        exitTransition = { uiScreenExit(tab = motion.isTab(initialState, targetState)) },
        popEnterTransition = { uiScreenEnter(false, motion.isTab(initialState, targetState)) },
        popExitTransition = { uiScreenExit(false, motion.isTab(initialState, targetState)) },
        builder = builder,
    )
}
