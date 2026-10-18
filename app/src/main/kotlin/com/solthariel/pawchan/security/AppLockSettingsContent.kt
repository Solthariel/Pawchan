package com.solthariel.pawchan.security

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.solthariel.pawchan.domain.security.AppLockDelay
import com.solthariel.pawchan.settings.SettingsItem
import com.solthariel.pawchan.settings.SettingsItemRow

@Composable
internal fun AppLockSettingsContent(
    state: AppLockState,
    fingerprintAvailable: Boolean,
    onDelay: (AppLockDelay) -> Unit = {},
    onEdit: (LockAction) -> Unit,
) {
    Column {
        SettingsItemRow(
            SettingsItem.Toggle(
                "Блокировка приложения",
                if (state.configuration.enabled) {
                    "Запрашивать PIN-код при открытии приложения"
                } else {
                    "Четырёхзначный PIN-код для Pawchan"
                },
                state.configuration.enabled,
            ) { onEdit(if (it) LockAction.ENABLE else LockAction.DISABLE) },
        )
        if (state.configuration.enabled) LockEnabledControls(state, fingerprintAvailable, onDelay, onEdit)
        if (state.editor == null && state.error != null) {
            Text(
                state.error,
                Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodyMedium,
            )
        }
        Text(
            "Забытый PIN-код восстановить нельзя.",
            Modifier.padding(start = 16.dp, end = 16.dp, bottom = 16.dp),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.bodyMedium,
        )
    }
}

@Composable
private fun LockEnabledControls(
    state: AppLockState,
    fingerprintAvailable: Boolean,
    onDelay: (AppLockDelay) -> Unit = {},
    onEdit: (LockAction) -> Unit,
) {
    HorizontalDivider(Modifier.padding(horizontal = 16.dp))
    SettingsItemRow(
        SettingsItem.Choice(
            "Блокировать после выхода",
            AppLockDelay.entries.map { it.label },
            AppLockDelay.entries.indexOf(state.configuration.delay),
        ) { onDelay(AppLockDelay.entries[it]) },
    )
    HorizontalDivider(Modifier.padding(horizontal = 16.dp))
    SettingsItemRow(SettingsItem.Action("Изменить PIN-код", null, onClick = { onEdit(LockAction.CHANGE) }))
    HorizontalDivider(Modifier.padding(horizontal = 16.dp))
    if (fingerprintAvailable || state.configuration.biometric) {
        SettingsItemRow(
            SettingsItem.Toggle(
                "Вход по отпечатку",
                "Использовать вместо PIN-кода",
                state.configuration.biometric,
            ) { onEdit(if (it) LockAction.BIOMETRIC_ENABLE else LockAction.BIOMETRIC_DISABLE) },
        )
    } else {
        SettingsItemRow(SettingsItem.Action("Вход по отпечатку", "Недоступен. Проверьте настройки телефона."))
    }
}
