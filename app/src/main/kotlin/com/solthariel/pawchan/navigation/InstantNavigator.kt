package com.solthariel.pawchan.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NamedNavArgument
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavDeepLink
import androidx.navigation.NavDestination
import androidx.navigation.NavDestinationBuilder
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavOptions
import androidx.navigation.Navigator

/** Keeps outgoing entries alive until the host has finished its transition. */
@Navigator.Name("pawchan_instant")
internal class InstantNavigator : Navigator<InstantDestination>() {
    override fun createDestination(): InstantDestination = InstantDestination(this) {}

    override fun navigate(
        entries: List<NavBackStackEntry>,
        navOptions: NavOptions?,
        navigatorExtras: Extras?,
    ) {
        entries.forEach { state.pushWithTransition(it) }
    }

    fun completeTransitions() {
        state.transitionsInProgress.value
            .toList()
            .forEach(state::markTransitionComplete)
    }

    override fun popBackStack(
        popUpTo: NavBackStackEntry,
        savedState: Boolean,
    ) {
        state.popWithTransition(popUpTo, savedState)
    }
}

internal class InstantDestination(
    navigator: InstantNavigator,
    val content: @Composable (NavBackStackEntry) -> Unit,
) : NavDestination(navigator)

private class InstantDestinationBuilder(
    navigator: InstantNavigator,
    route: String,
    private val content: @Composable (NavBackStackEntry) -> Unit,
) : NavDestinationBuilder<InstantDestination>(navigator, route) {
    private val instantNavigator = navigator

    override fun instantiateDestination(): InstantDestination = InstantDestination(instantNavigator, content)
}

internal fun NavGraphBuilder.appComposable(
    route: String,
    arguments: List<NamedNavArgument> = emptyList(),
    deepLinks: List<NavDeepLink> = emptyList(),
    content: @Composable (NavBackStackEntry) -> Unit,
) {
    val builder = InstantDestinationBuilder(provider.getNavigator(InstantNavigator::class.java), route, content)
    arguments.forEach { builder.argument(it.name, it.argument) }
    deepLinks.forEach { builder.deepLink(it) }
    destination(builder)
}
