package com.solthariel.pawchan.thread

import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import com.solthariel.pawchan.core.ui.UiIconKind
import com.solthariel.pawchan.core.ui.UiToolbarButton

/** Scoped display input, not access to workspace or a global state owner. */
internal val LocalThreadUnreadIndicator = staticCompositionLocalOf<@Composable () -> Boolean> { { false } }

@Composable
internal fun ThreadToolbarButton(
    kind: UiIconKind,
    description: String,
    rotationDegrees: Float = 0f,
    enabled: Boolean = true,
    active: Boolean = false,
    onClick: () -> Unit,
) {
    UiToolbarButton(
        kind,
        description,
        rotationDegrees,
        enabled,
        threadToolbarActive(kind, active, LocalThreadUnreadIndicator.current()),
        onClick,
    )
}

internal fun threadToolbarActive(
    kind: UiIconKind,
    active: Boolean,
    unread: Boolean,
): Boolean = active || (unread && (kind == UiIconKind.BACK || kind == UiIconKind.MENU))
