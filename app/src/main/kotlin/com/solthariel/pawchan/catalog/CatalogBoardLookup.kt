package com.solthariel.pawchan.catalog

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.solthariel.pawchan.core.ui.APP_TOP_BAR_HEIGHT
import com.solthariel.pawchan.core.ui.UiIconKind
import com.solthariel.pawchan.core.ui.UiToolbarButton
import com.solthariel.pawchan.core.ui.UiToolbarSearchField
import com.solthariel.pawchan.core.ui.modalPanelAdditionalBounds

@Composable
internal fun CatalogBoardLookupToolbar(
    lookup: CatalogBoardLookupBindings,
    placeholder: String = "Введите доску…",
) {
    val focus = remember { FocusRequester() }
    val keyboard = LocalSoftwareKeyboardController.current
    BackHandler(onBack = lookup.onDismiss)
    LaunchedEffect(Unit) {
        focus.requestFocus()
        keyboard?.show()
    }
    Row(
        Modifier
            .fillMaxWidth()
            .modalPanelAdditionalBounds()
            .heightIn(min = APP_TOP_BAR_HEIGHT)
            .testTag("board-lookup-toolbar")
            .padding(horizontal = com.solthariel.pawchan.core.ui.APP_TOOLBAR_HORIZONTAL_INSET),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        UiToolbarButton(UiIconKind.BACK, "Закрыть выбор доски", onClick = lookup.onDismiss)
        UiToolbarSearchField(
            lookup.query,
            lookup.onQueryChange,
            focus,
            Modifier.weight(1f).testTag("board-lookup-input"),
            placeholder = placeholder,
        )
    }
}
