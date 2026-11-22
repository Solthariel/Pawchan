package com.solthariel.pawchan.core.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp

/** One fixed surface shared by all cells in a choice row. */
@Composable
fun Modifier.uiChoiceGroup(): Modifier {
    val surface = MaterialTheme.colorScheme.surfaceContainerLow
    return clip(RoundedCornerShape(12.dp)).background(surface)
}
