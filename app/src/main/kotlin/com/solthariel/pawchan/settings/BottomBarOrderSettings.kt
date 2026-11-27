package com.solthariel.pawchan.settings

import android.content.res.Resources
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.solthariel.pawchan.R
import com.solthariel.pawchan.core.ui.AppAlertDialog
import com.solthariel.pawchan.core.ui.UiForwardIndicator
import com.solthariel.pawchan.domain.settings.AppSettings
import com.solthariel.pawchan.domain.settings.BottomBarButton
import com.solthariel.pawchan.domain.settings.NavigationPreferences
import com.solthariel.pawchan.domain.settings.normalizedBottomBarOrder
import com.solthariel.pawchan.R.string as L
import com.solthariel.pawchan.core.ui.R as CoreUiR

/** «Интерфейс → Нижняя панель»: порядок пяти кнопок; по умолчанию — прежний. */
@Composable
internal fun bottomBarOrderGroup(
    app: AppSettings,
    update: ((AppSettings) -> AppSettings) -> Unit,
): SettingsGroup {
    val res = LocalResources.current
    val order =
        app.behavior.client.reading.navigation
            .normalized()
            .bottomBarOrder
    return SettingsGroup(
        res.getString(L.ui_bottom_bar_group),
        listOf(
            SettingsItem.Custom(res.getString(L.ui_button_order)) {
                BottomBarOrderRow(order) { chosen -> update { it.withBottomBarOrder(chosen) } }
            },
        ),
    )
}

/** «Интерфейс → Клавиатура»: одна настройка на все поля, ради которых открывают экран. */
@Composable
internal fun keyboardSettingsGroup(
    app: AppSettings,
    update: ((AppSettings) -> AppSettings) -> Unit,
): SettingsGroup {
    val res = LocalResources.current
    return SettingsGroup(
        res.getString(L.ui_keyboard_group),
        listOf(
            SettingsItem.Toggle(
                res.getString(L.ui_auto_open_keyboard),
                res.getString(L.ui_auto_open_keyboard_summary),
                app.behavior.client.reading.navigation.autoOpenKeyboard,
            ) { on -> update { it.withAutoOpenKeyboard(on) } },
        ),
    )
}

@Composable
private fun BottomBarOrderRow(
    order: List<BottomBarButton>,
    onChange: (List<BottomBarButton>) -> Unit,
) {
    val res = LocalResources.current
    var open by rememberSaveable { mutableStateOf(false) }
    SettingsRow(
        res.getString(L.ui_button_order),
        order.joinToString(" · ") { res.bottomBarLabel(it) },
        onClick = { open = true },
    ) { UiForwardIndicator() }
    if (open) BottomBarOrderDialog(order, onChange) { open = false }
}

@Composable
internal fun BottomBarOrderDialog(
    order: List<BottomBarButton>,
    onConfirm: (List<BottomBarButton>) -> Unit,
    onDismiss: () -> Unit,
) {
    val res = LocalResources.current
    var draft by remember(order) { mutableStateOf(normalizedBottomBarOrder(order)) }
    AppAlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(res.getString(L.ui_button_order)) },
        text = {
            Column(Modifier.testTag("bottom-bar-order")) {
                draft.forEachIndexed { index, button ->
                    ReorderRow(
                        res.bottomBarLabel(button),
                        index,
                        draft.size,
                        { delta -> draft = draft.moved(index, delta) },
                        leading = {
                            Icon(
                                painterResource(button.iconResource()),
                                contentDescription = null,
                                modifier = Modifier.size(24.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        },
                    )
                }
            }
        },
        dismissButton = {
            TextButton(onClick = { draft = BottomBarButton.entries }) { Text(res.getString(L.ui_reset)) }
        },
        confirmButton = {
            TextButton(onClick = {
                onConfirm(draft)
                onDismiss()
            }) { Text(res.getString(L.ui_done)) }
        },
    )
}

internal fun Resources.bottomBarLabel(button: BottomBarButton): String =
    getString(
        when (button) {
            BottomBarButton.CATALOG -> R.string.catalog
            BottomBarButton.THREADS -> R.string.threads_hub
            BottomBarButton.BOARDS -> R.string.boards
            BottomBarButton.SETTINGS -> R.string.settings_short
            BottomBarButton.WRITE -> R.string.write
        },
    )

private fun BottomBarButton.iconResource(): Int =
    when (this) {
        BottomBarButton.CATALOG -> R.drawable.ic_nav_catalog
        BottomBarButton.THREADS -> R.drawable.ic_nav_threads
        BottomBarButton.BOARDS -> R.drawable.ic_nav_boards
        BottomBarButton.SETTINGS -> R.drawable.ic_nav_settings
        BottomBarButton.WRITE -> CoreUiR.drawable.ic_rounded_edit_square
    }

internal fun AppSettings.withBottomBarOrder(order: List<BottomBarButton>): AppSettings =
    withNavigation { it.copy(layout = it.layout.copy(bottomBar = order)) }

internal fun AppSettings.withAutoOpenKeyboard(on: Boolean): AppSettings =
    withNavigation { navigation ->
        navigation.copy(autoOpenKeyboard = on)
    }

private fun AppSettings.withNavigation(change: (NavigationPreferences) -> NavigationPreferences): AppSettings =
    withClient { client ->
        client.copy(reading = client.reading.copy(navigation = change(client.reading.navigation).normalized()))
    }
