package com.solthariel.pawchan.navigation

import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavController

/** Owned by the NavHost; only the most recent explicit bottom-tab move is remembered. */
internal class AppNavigationMotion {
    private var tabPair: Pair<String?, String?>? = null

    fun tab(
        navController: NavController,
        navigate: () -> Unit,
    ) {
        val from = navController.currentBackStackEntry?.id
        navigate()
        tabPair = from to navController.currentBackStackEntry?.id
    }

    fun isTab(
        from: NavBackStackEntry,
        to: NavBackStackEntry,
    ): Boolean = usesTabMotion(from.destination.route, to.destination.route, tabPair == (from.id to to.id))
}

internal fun usesTabMotion(
    from: String?,
    to: String?,
    explicitTab: Boolean,
): Boolean = explicitTab || (from in ROOT_ROUTES && to in ROOT_ROUTES)

private val ROOT_ROUTES = setOf(AppRoute.CATALOG, AppRoute.BOARDS, AppRoute.LIBRARY, AppRoute.SETTINGS)
