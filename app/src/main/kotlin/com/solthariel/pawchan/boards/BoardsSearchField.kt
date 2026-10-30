package com.solthariel.pawchan.boards

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import com.solthariel.pawchan.R
import com.solthariel.pawchan.catalog.CatalogIconButton
import com.solthariel.pawchan.catalog.CatalogRefreshButton
import com.solthariel.pawchan.core.ui.ActionMenuItem
import com.solthariel.pawchan.core.ui.UiIconKind
import com.solthariel.pawchan.core.ui.UiTopAppBar

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
    UiTopAppBar(
        title = { BoardsSearchTextField(state, actions) },
        navigationIcon = {
            CatalogIconButton(UiIconKind.BACK, stringResource(R.string.board_dialog_close), onClick = close)
        },
        actions = {
            CatalogRefreshButton(state.refreshing, actions.refresh, enabled = !state.refreshing)
            Box {
                CatalogIconButton(UiIconKind.MORE, "Меню досок", onClick = { overflow = true })
                DropdownMenu(expanded = overflow, onDismissRequest = { overflow = false }) {
                    ActionMenuItem(stringResource(R.string.refresh), {
                        overflow = false
                        actions.refresh()
                    })
                }
            }
        },
    )
}

@Composable
private fun BoardsSearchTextField(
    state: BoardsSearchBarState,
    actions: BoardsSearchBarActions,
) {
    TextField(
        value = state.query,
        onValueChange = actions.change,
        modifier = Modifier.fillMaxWidth().testTag("boards-search"),
        singleLine = true,
        textStyle = MaterialTheme.typography.bodyLarge.copy(color = MaterialTheme.colorScheme.onSurface),
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Go),
        keyboardActions =
            KeyboardActions(
                onGo = { actions.submit() },
                onDone = { actions.submit() },
                onSearch = { actions.submit() },
            ),
        placeholder = { Text(stringResource(R.string.boards_address_hint), maxLines = 1) },
        shape = MaterialTheme.shapes.extraLarge,
        trailingIcon =
            if (state.query.isEmpty()) {
                null
            } else {
                {
                    CatalogIconButton(
                        UiIconKind.CLOSE,
                        stringResource(R.string.clear_search),
                        onClick = { actions.change("") },
                    )
                }
            },
    )
}
