package com.solthariel.pawchan.core.ui

import androidx.compose.material3.Switch
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/** Material switch; the owning row remains the only accessible toggle target. */
@Composable
fun CompactSwitch(
    checked: Boolean,
    modifier: Modifier = Modifier,
) {
    Switch(checked = checked, onCheckedChange = null, modifier = modifier)
}
