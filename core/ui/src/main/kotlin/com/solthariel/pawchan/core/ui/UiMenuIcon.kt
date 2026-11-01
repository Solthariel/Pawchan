package com.solthariel.pawchan.core.ui

import androidx.compose.foundation.layout.size
import androidx.compose.material3.LocalContentColor
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/** Decorative child: the enclosing menu row owns semantics and interaction. */
@Composable
fun UiMenuIcon(kind: UiIconKind) {
    UiIcon(kind, LocalContentColor.current, Modifier.size(24.dp))
}
