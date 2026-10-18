package com.solthariel.pawchan.catalog

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.delay

/** Refreshes prepared labels, not individual rows; no network request or frame-by-frame timer. */
@Composable
internal fun BindCatalogAgeUpdates(viewModel: CatalogViewModel) {
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    LaunchedEffect(lifecycle, viewModel) {
        lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
            while (true) {
                viewModel.refreshThreadAges()
                delay(CATALOG_AGE_UPDATE_MILLIS)
            }
        }
    }
}

private const val CATALOG_AGE_UPDATE_MILLIS = 60_000L
