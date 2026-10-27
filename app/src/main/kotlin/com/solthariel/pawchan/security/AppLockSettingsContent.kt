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
import com.solthariel.pawchan.domain.security.appLockDelayChoices
import com.solthariel.pawchan.settings.SettingsItem
import com.solthariel.pawchan.settings.SettingsItemRow

@Composable
internal fun AppLockSettingsContent(
    state: AppLockSettingsState,
    fingerprintAvailable: Boolean,
    onDelay: (AppLockDelay) -> Unit = {},
    onEdit: (LockAction) -> Unit,
) {
    Column {
        SettingsItemRow(
            SettingsItem.Toggle(
                "Блокировка приложения",
                if (state.enabled) {
                    "Запрашивать PIN-код при открытии приложения"
                } else {
                    "Четырёхзначный PIN-код для Pawchan"
                },
                state.enabled,
            ) { onEdit(if (it) LockAction.ENABLE else LockAction.DISABLE) },
        )
        if (state.enabled) LockEnabledControls(state, fingerprintAvailable, onDelay, onEdit)
        val error = state.error
        if (!state.editing && error != null) {
            Text(
                error,
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
    state: AppLockSettingsState,
    fingerprintAvailable: Boolean,
    onDelay: (AppLockDelay) -> Unit = {},
    onEdit: (LockAction) -> Unit,
) {
    val delays = appLockDelayChoices(state.delay)
    HorizontalDivider(Modifier.padding(horizontal = 16.dp))
    SettingsItemRow(
        SettingsItem.Choice(
            "Запрашивать PIN после отсутствия",
            delays.map { it.label },
            delays.indexOf(state.delay),
        ) { onDelay(delays[it]) },
    )
    HorizontalDivider(Modifier.padding(horizontal = 16.dp))
    SettingsItemRow(SettingsItem.Action("Изменить PIN-код", null, onClick = { onEdit(LockAction.CHANGE) }))
    HorizontalDivider(Modifier.padding(horizontal = 16.dp))
    if (fingerprintAvailable || state.biometric) {
        SettingsItemRow(
            SettingsItem.Toggle(
                "Вход по отпечатку",
                "Использовать вместо PIN-кода",
                state.biometric,
            ) { onEdit(if (it) LockAction.BIOMETRIC_ENABLE else LockAction.BIOMETRIC_DISABLE) },
        )
    } else {
        SettingsItemRow(SettingsItem.Action("Вход по отпечатку", "Недоступен. Проверьте настройки телефона."))
    }
}
