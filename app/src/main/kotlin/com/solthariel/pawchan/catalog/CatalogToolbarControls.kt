package com.solthariel.pawchan.catalog

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.solthariel.pawchan.R
import com.solthariel.pawchan.core.ui.UiIconKind
import com.solthariel.pawchan.core.ui.UiRefreshButton
import com.solthariel.pawchan.core.ui.UiToolbarButton

@Composable
internal fun CatalogRefreshButton(
    isRefreshing: Boolean,
    onClick: () -> Unit,
    enabled: Boolean = true,
) {
    UiRefreshButton(isRefreshing, onClick, stringResource(R.string.refresh), enabled)
}

@Composable
internal fun CatalogIconButton(
    kind: UiIconKind,
    description: String,
    rotationDegrees: Float = 0f,
    enabled: Boolean = true,
    active: Boolean = false,
    onClick: () -> Unit,
) {
    UiToolbarButton(kind, description, rotationDegrees, enabled, active, onClick)
}
