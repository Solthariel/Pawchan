package com.solthariel.pawchan.boards

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.solthariel.pawchan.R
import com.solthariel.pawchan.catalog.CatalogIconButton
import com.solthariel.pawchan.catalog.CatalogRefreshButton
import com.solthariel.pawchan.core.ui.APP_TOOLBAR_BUTTON_SIZE
import com.solthariel.pawchan.core.ui.APP_TOOLBAR_HORIZONTAL_INSET
import com.solthariel.pawchan.core.ui.APP_TOP_BAR_HEIGHT
import com.solthariel.pawchan.core.ui.ActionMenuItem
import com.solthariel.pawchan.core.ui.UiIcon
import com.solthariel.pawchan.core.ui.UiIconKind
import com.solthariel.pawchan.core.ui.ModalDropdownMenu as DropdownMenu

@Composable
internal fun BoardsSearchField(
    state: BoardsSearchBarState,
    actions: BoardsSearchBarActions,
) {
    var overflow by remember { mutableStateOf(false) }
    val focus = LocalFocusManager.current
    val keyboard = LocalSoftwareKeyboardController.current
    val close = {
        focus.clearFocus()
        keyboard?.hide()
        actions.back()
    }
    BackHandler(onBack = close)
    Box(
        Modifier.fillMaxWidth().heightIn(min = APP_TOP_BAR_HEIGHT),
        contentAlignment = Alignment.Center,
    ) {
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .heightIn(min = APP_TOOLBAR_BUTTON_SIZE)
                    .padding(horizontal = APP_TOOLBAR_HORIZONTAL_INSET),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            CatalogIconButton(UiIconKind.BACK, stringResource(R.string.board_dialog_close), onClick = close)
            BoardsSearchInput(state, actions)
            CatalogRefreshButton(state.refreshing, actions.refresh, enabled = !state.refreshing)
            Box {
                CatalogIconButton(
                    UiIconKind.MORE,
                    "Меню досок",
                    onClick = { overflow = true },
                )
                DropdownMenu(expanded = overflow, onDismissRequest = { overflow = false }) {
                    ActionMenuItem(stringResource(R.string.refresh), {
                        overflow = false
                        actions.refresh()
                    })
                }
            }
        }
    }
}

@Composable
private fun androidx.compose.foundation.layout.RowScope.BoardsSearchInput(
    state: BoardsSearchBarState,
    actions: BoardsSearchBarActions,
) {
    com.solthariel.pawchan.core.ui.UiToolbarSearchLayout(
        Modifier.weight(1f).padding(horizontal = 4.dp),
        if (state.query.isNotEmpty()) ({ actions.change("") }) else null,
        stringResource(R.string.clear_search),
    ) {
        BoardsSearchTextField(state, actions)
    }
}

@Composable
private fun androidx.compose.foundation.layout.RowScope.BoardsSearchTextField(
    state: BoardsSearchBarState,
    actions: BoardsSearchBarActions,
) {
    BasicTextField(
        value = state.query,
        onValueChange = actions.change,
        modifier = Modifier.weight(1f).testTag("boards-search"),
        singleLine = true,
        textStyle = MaterialTheme.typography.bodyLarge.copy(color = MaterialTheme.colorScheme.onSurface),
        cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Go),
        keyboardActions =
            KeyboardActions(
                onGo = { actions.submit() },
                onDone = { actions.submit() },
                onSearch = { actions.submit() },
            ),
        decorationBox = { inner ->
            if (state.query.isEmpty()) {
                Text(
                    stringResource(R.string.boards_address_hint),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodyLarge,
                )
            }
            inner()
        },
    )
}
