package com.solthariel.pawchan.catalog

import androidx.compose.foundation.layout.Box
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import com.solthariel.pawchan.R
import com.solthariel.pawchan.core.ui.OVAL_BUTTON_SHAPE
import com.solthariel.pawchan.core.ui.UiIconKind
import com.solthariel.pawchan.domain.settings.ThemePreference
import com.solthariel.pawchan.settings.labelResource

@Composable
internal fun CatalogOverflowMenu(
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    menu: CatalogMenuBindings,
) {
    var appearance by rememberSaveable { mutableStateOf(false) }
    val close = {
        appearance = false
        onExpandedChange(false)
    }
    Box {
        CatalogIconButton(UiIconKind.MORE, stringResource(R.string.more_actions)) {
            appearance = false
            onExpandedChange(true)
        }
        DropdownMenu(expanded, onDismissRequest = close) {
            if (appearance) {
                AppearanceMenu(menu, onBack = { appearance = false }, onClose = close)
            } else {
                BoardMenuItems(menu, onAppearance = { appearance = true }, onClose = close)
            }
        }
    }
}

@Composable
private fun BoardMenuItems(
    menu: CatalogMenuBindings,
    onAppearance: () -> Unit,
    onClose: () -> Unit,
) {
    val prefs = menu.state.preferences
    CatalogHiddenThreadItems(menu.hiddenThreads, onClose)
    BoardMenuItem(R.string.board_archive, onClose, menu.navigation.onArchive)
    BoardMenuItem(R.string.new_thread_short, onClose, menu.navigation.onNewThread)
    BoardMenuItem(R.string.board_information, onClose, menu.navigation.onInformation)
    DropdownMenuItem(
        modifier = Modifier.clip(OVAL_BUTTON_SHAPE),
        text = { Text(stringResource(R.string.board_appearance)) },
        trailingIcon = { Text("›") },
        onClick = onAppearance,
    )
    BoardMenuItem(
        if (prefs.favorite) R.string.remove_favorite_board else R.string.add_favorite_board,
        onClose,
        menu.preferences.onFavorite,
        enabled = !prefs.saving,
    )
    BoardMenuItem(
        if (prefs.home) R.string.board_is_home else R.string.make_home_board,
        onClose,
        menu.preferences.onHome,
        enabled = !prefs.saving && !prefs.home,
    )
}

@Composable
private fun AppearanceMenu(
    menu: CatalogMenuBindings,
    onBack: () -> Unit,
    onClose: () -> Unit,
) {
    DropdownMenuItem(
        modifier = Modifier.clip(OVAL_BUTTON_SHAPE),
        text = { Text(stringResource(R.string.board_appearance)) },
        leadingIcon = { Text("‹") },
        onClick = onBack,
    )
    ThemePreference.entries.forEach { theme ->
        DropdownMenuItem(
            modifier = Modifier.clip(OVAL_BUTTON_SHAPE),
            text = { Text(stringResource(theme.labelResource)) },
            leadingIcon = { RadioButton(theme == menu.state.preferences.theme, onClick = null) },
            enabled = !menu.state.preferences.saving,
            onClick = {
                onClose()
                menu.preferences.onTheme(theme)
            },
        )
    }
}

@Composable
private fun BoardMenuItem(
    label: Int,
    onClose: () -> Unit,
    onClick: () -> Unit,
    enabled: Boolean = true,
) {
    DropdownMenuItem(
        modifier = Modifier.clip(OVAL_BUTTON_SHAPE),
        text = { Text(stringResource(label)) },
        enabled = enabled,
        onClick = {
            onClose()
            onClick()
        },
    )
}
