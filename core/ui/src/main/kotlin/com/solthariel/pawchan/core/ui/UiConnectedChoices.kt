package com.solthariel.pawchan.core.ui

import androidx.compose.runtime.Composable

/** Equal-width choices share the outlined selection style with other option groups. */
@Composable
fun UiConnectedChoices(
    options: List<String>,
    selected: Int,
    enabled: Boolean,
    lastOptionDescription: String? = null,
    onSelect: (Int) -> Unit,
) {
    UiChoiceSegments(options, selected, enabled, lastOptionDescription, onSelect = onSelect)
}
