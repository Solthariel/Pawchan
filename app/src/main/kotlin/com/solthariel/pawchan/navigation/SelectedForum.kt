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
