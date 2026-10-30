package com.solthariel.pawchan.core.ui

import androidx.compose.material3.AlertDialogDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp

/** Shared tokens for centered windows; full-screen viewers and anchored menus are distinct surfaces. */
object AppDialogStyle {
    val shape
        @Composable get() = AlertDialogDefaults.shape
    val maxWidth = 560.dp
    val screenPadding = 24.dp
    val contentPadding = 24.dp
}
