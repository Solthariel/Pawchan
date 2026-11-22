package com.solthariel.pawchan.navigation

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.updateTransition
import androidx.compose.animation.togetherWith
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.runtime.setValue
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.LocalOwnersProvider
import androidx.navigation.createGraph
import com.solthariel.pawchan.core.ui.uiScreenEnter
import com.solthariel.pawchan.core.ui.uiScreenExit

/** Animates only the Threads hub and its three child screens. */
@Composable
internal fun AppAnimatedNavHost(
    controller: NavHostController,
    startRoute: String,
    builder: NavGraphBuilder.() -> Unit,
) {
    val lifecycleOwner = LocalLifecycleOwner.current
    val storeOwner = checkNotNull(LocalViewModelStoreOwner.current)
    controller.setViewModelStore(storeOwner.viewModelStore)
    val graph = remember(controller, startRoute, builder) { controller.createGraph(startRoute, builder = builder) }
    controller.graph = graph
    DisposableEffect(controller, lifecycleOwner) {
        controller.setLifecycleOwner(lifecycleOwner)
        onDispose { }
    }
    var current by remember(controller) { mutableStateOf(controller.currentBackStackEntry) }
    var sceneGroup by remember(controller) { mutableIntStateOf(0) }
    DisposableEffect(controller) {
        val listener =
            NavController.OnDestinationChangedListener { nav, _, _ ->
                val next = nav.currentBackStackEntry
                val direction = threadsScreenMotion(current?.destination?.route, next?.destination?.route)
                if (current?.id != next?.id && direction == null) {
                    sceneGroup++
                }
                current = next
            }
        controller.addOnDestinationChangedListener(listener)
        onDispose { controller.removeOnDestinationChangedListener(listener) }
    }
    val holder = rememberSaveableStateHolder()
    val navigator = remember(controller) { controller.navigatorProvider.getNavigator(InstantNavigator::class.java) }
    BackHandler(enabled = current != null && controller.previousBackStackEntry != null) { controller.popBackStack() }
    // Restart only the animation clock on instant jumps. A generation key around the destination
    // would also change rememberSaveable's positional keys when the same back-stack entry returns.
    val transition = key(sceneGroup) { updateTransition(current, label = "threads-navigation") }
    transition.AnimatedContent(transitionSpec = {
        val direction = threadsScreenMotion(initialState?.destination?.route, targetState?.destination?.route)
        if (direction == null) {
            EnterTransition.None.togetherWith(ExitTransition.None)
        } else {
            uiScreenEnter(forward = direction).togetherWith(uiScreenExit(forward = direction))
        }
    }, contentKey = { it?.id }) { entry ->
        entry?.LocalOwnersProvider(holder) { (entry.destination as InstantDestination).content(entry) }
    }
    LaunchedEffect(transition, transition.currentState, transition.targetState) {
        if (transition.currentState == transition.targetState &&
            controller.currentBackStackEntry == transition.targetState
        ) {
            navigator.completeTransitions()
        }
    }
    DisposableEffect(navigator) { onDispose { navigator.completeTransitions() } }
}

/** true = forward, false = back, null = no animation. */
internal fun threadsScreenMotion(
    from: String?,
    to: String?,
): Boolean? =
    when {
        from == AppRoute.LIBRARY && to in THREAD_DETAIL_ROUTES -> true
        to == AppRoute.LIBRARY && from in THREAD_DETAIL_ROUTES -> false
        else -> null
    }

internal val THREAD_DETAIL_ROUTES = setOf(AppRoute.HISTORY, AppRoute.COPIES, AppRoute.DOWNLOADS)
internal val THREAD_WORKSPACE_ROUTES = THREAD_DETAIL_ROUTES + AppRoute.LIBRARY
