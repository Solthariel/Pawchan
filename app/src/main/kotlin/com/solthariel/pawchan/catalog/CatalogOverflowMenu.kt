package com.solthariel.pawchan.catalog

import androidx.compose.foundation.layout.Box
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
import com.solthariel.pawchan.core.ui.UiBackwardIndicator
import com.solthariel.pawchan.core.ui.UiIconKind
import com.solthariel.pawchan.core.ui.UiMenuIcon
import com.solthariel.pawchan.core.ui.UiSubmenuIndicator
import com.solthariel.pawchan.domain.settings.ThemePreference
import com.solthariel.pawchan.domain.settings.displayMode
import com.solthariel.pawchan.settings.labelResource
import com.solthariel.pawchan.core.ui.ModalDropdownMenu as DropdownMenu

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
    BoardMenuItem(
        R.string.board_archive,
        onClose,
        menu.navigation.onArchive,
        enabled = menu.capabilities.boardArchive,
        icon = UiIconKind.ARCHIVE,
    )
    menu.navigation.onArhivach?.let { open ->
        BoardMenuItem(R.string.arhivach_menu, onClose, open, icon = UiIconKind.ARCHIVE)
    }
    BoardMenuItem(
        R.string.new_thread_short,
        onClose,
        menu.navigation.onNewThread,
        enabled = menu.capabilities.newThreads,
        icon = UiIconKind.WRITE,
    )
    BoardMenuItem(
        R.string.board_information,
        onClose,
        menu.navigation.onInformation,
        enabled = menu.capabilities.boardInformation,
        icon = UiIconKind.INFO,
    )
    DropdownMenuItem(
        modifier = Modifier.clip(OVAL_BUTTON_SHAPE),
        text = { Text(stringResource(R.string.board_appearance)) },
        leadingIcon = { UiMenuIcon(UiIconKind.PALETTE) },
        trailingIcon = { UiSubmenuIndicator() },
        onClick = onAppearance,
    )
    BoardMenuItem(
        if (prefs.favorite) R.string.remove_favorite_board else R.string.add_favorite_board,
        onClose,
        menu.preferences.onFavorite,
        enabled = !prefs.saving,
        icon = if (prefs.favorite) UiIconKind.STAR_FILLED else UiIconKind.STAR,
    )
    BoardMenuItem(
        if (prefs.home) R.string.board_is_home else R.string.make_home_board,
        onClose,
        menu.preferences.onHome,
        enabled = !prefs.saving && !prefs.home,
        icon = UiIconKind.HOME,
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
        leadingIcon = { UiBackwardIndicator() },
        onClick = onBack,
    )
    val modes = listOf(ThemePreference.SYSTEM, ThemePreference.LIGHT, ThemePreference.DARK, ThemePreference.AMOLED)
    modes.forEach { theme ->
        DropdownMenuItem(
            modifier = Modifier.clip(OVAL_BUTTON_SHAPE),
            text = { Text(stringResource(theme.labelResource)) },
            leadingIcon = {
                RadioButton(
                    theme ==
                        menu.state.preferences.theme
                            .let { if (it == ThemePreference.AMOLED) it else it.displayMode() },
                    onClick = null,
                )
            },
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
    icon: UiIconKind? = null,
) {
    DropdownMenuItem(
        modifier = Modifier.clip(OVAL_BUTTON_SHAPE),
        text = { Text(stringResource(label)) },
        leadingIcon = icon?.let { { UiMenuIcon(it) } },
        enabled = enabled,
        onClick = {
            onClose()
            onClick()
        },
    )
}
