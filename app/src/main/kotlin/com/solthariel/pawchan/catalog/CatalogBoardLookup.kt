package com.solthariel.pawchan.catalog

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import com.solthariel.pawchan.core.ui.UiIconKind
import com.solthariel.pawchan.core.ui.UiToolbarButton
import com.solthariel.pawchan.core.ui.UiToolbarSearchField
import com.solthariel.pawchan.core.ui.UiTopAppBar

@Composable
internal fun CatalogBoardLookupToolbar(lookup: CatalogBoardLookupBindings) {
    val focus = remember { FocusRequester() }
    val keyboard = LocalSoftwareKeyboardController.current
    BackHandler(onBack = lookup.onDismiss)
    LaunchedEffect(Unit) {
        focus.requestFocus()
        keyboard?.show()
    }
    UiTopAppBar(
        navigationIcon = { UiToolbarButton(UiIconKind.BACK, "Закрыть выбор доски", onClick = lookup.onDismiss) },
        title = {
            UiToolbarSearchField(
                lookup.query,
                lookup.onQueryChange,
                focus,
                Modifier.fillMaxWidth().testTag("board-lookup-input"),
                placeholder = "Введите доску…",
            )
        },
    )
}
