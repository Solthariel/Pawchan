package com.solthariel.pawchan.security

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.unit.dp
import com.solthariel.pawchan.core.ui.TransientNotice
import com.solthariel.pawchan.domain.security.AppLockDelay
import com.solthariel.pawchan.domain.security.appLockDelayChoices
import com.solthariel.pawchan.settings.SettingsItem
import com.solthariel.pawchan.settings.SettingsItemRow
import com.solthariel.pawchan.R.string as L

@Composable
internal fun AppLockSettingsContent(
    state: AppLockSettingsState,
    fingerprintAvailable: Boolean,
    onDelay: (AppLockDelay) -> Unit = {},
    onEdit: (LockAction) -> Unit,
) {
    val res = LocalResources.current

    Column {
        SettingsItemRow(
            SettingsItem.Toggle(
                res.getString(L.ui_app_lock),
                if (state.enabled) {
                    res.getString(L.ui_ask_for_pin_on_login)
                } else {
                    res.getString(L.ui_four_digit_pin_protection)
                },
                state.enabled,
            ) { onEdit(if (it) LockAction.ENABLE else LockAction.DISABLE) },
        )
        if (state.enabled) LockEnabledControls(state, fingerprintAvailable, onDelay, onEdit)
        val error = state.error
        if (!state.editing && error != null) {
            TransientNotice(error)
        }
        Text(
            res.getString(L.ui_a_forgotten_pin_cannot_be),
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
    val res = LocalResources.current

    val delays = appLockDelayChoices(state.delay)
    HorizontalDivider(Modifier.padding(horizontal = 16.dp))
    SettingsItemRow(
        SettingsItem.Choice(
            res.getString(L.ui_lock_after_exit),
            delays.map { it.label },
            delays.indexOf(state.delay),
        ) { onDelay(delays[it]) },
    )
    HorizontalDivider(Modifier.padding(horizontal = 16.dp))
    SettingsItemRow(
        SettingsItem.Action(res.getString(L.ui_change_pin), null, onClick = { onEdit(LockAction.CHANGE) }),
    )
    HorizontalDivider(Modifier.padding(horizontal = 16.dp))
    if (fingerprintAvailable || state.biometric) {
        SettingsItemRow(
            SettingsItem.Toggle(
                res.getString(L.ui_fingerprint_login),
                res.getString(L.ui_instead_of_pin),
                state.biometric,
            ) { onEdit(if (it) LockAction.BIOMETRIC_ENABLE else LockAction.BIOMETRIC_DISABLE) },
        )
    } else {
        SettingsItemRow(
            SettingsItem.Action(res.getString(L.ui_fingerprint_login), res.getString(L.ui_unavailable_on_device)),
        )
    }
}
