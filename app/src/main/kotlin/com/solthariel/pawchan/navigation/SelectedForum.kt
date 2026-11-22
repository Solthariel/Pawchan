package com.solthariel.pawchan.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.staticCompositionLocalOf

/** The selected directory can differ from the currently open catalog until a board is chosen. */
internal val LocalSelectedForum = staticCompositionLocalOf<MutableState<String>?> { null }

@Composable
internal fun rememberForumSelection(
    baseRoute: String,
    currentCatalogRoute: String?,
): Pair<State<String>, MutableState<String>> {
    val lastCatalog = rememberSaveable { mutableStateOf(baseRoute) }
    val selected = rememberSaveable { mutableStateOf(requireNotNull(AppRoute.catalogBoard(baseRoute)).site.value) }
    LaunchedEffect(currentCatalogRoute) {
        currentCatalogRoute?.let { route ->
            if (route != lastCatalog.value) {
                selected.value = AppRoute.catalogBoard(route)?.site?.value ?: selected.value
            }
            lastCatalog.value = route
        }
    }
    return lastCatalog to selected
}

/** Last catalog board for each forum, retained across activity recreation. */
@Composable
internal fun rememberForumCatalogs(
    baseRoute: String,
    currentCatalogRoute: String?,
): MutableState<String> {
    val routes = rememberSaveable { mutableStateOf(baseRoute) }
    LaunchedEffect(currentCatalogRoute) {
        currentCatalogRoute?.let { route ->
            val site = AppRoute.catalogBoard(route)?.site?.value ?: return@let
            routes.value =
                (routes.value.lineSequence().filter { AppRoute.catalogBoard(it)?.site?.value != site } + route)
                    .joinToString("\n")
        }
    }
    return routes
}

internal fun forumCatalogRoute(
    routes: String,
    site: String,
): String? = routes.lineSequence().firstOrNull { AppRoute.catalogBoard(it)?.site?.value == site }
