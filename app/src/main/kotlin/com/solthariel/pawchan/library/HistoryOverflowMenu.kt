package com.solthariel.pawchan.library

import androidx.compose.foundation.layout.Box
import androidx.compose.material3.DropdownMenu
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import com.solthariel.pawchan.catalog.CatalogIconButton
import com.solthariel.pawchan.core.ui.UiIconKind

@Composable
internal fun HistoryOverflowMenu(onCleared: () -> Unit) {
    var open by remember { mutableStateOf(false) }
    Box {
        CatalogIconButton(UiIconKind.MORE, "Меню истории", onClick = { open = true })
        DropdownMenu(open, onDismissRequest = { open = false }) { ClearVisitHistory(onCleared) }
    }
}
