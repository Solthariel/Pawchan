package com.solthariel.pawchan.security

import android.app.Activity
import androidx.activity.compose.LocalActivity
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.solthariel.pawchan.core.ui.AppDialog

@Composable
fun AppLockSettingsHost(
    content: @Composable (
        state: AppLockSettingsState,
        fingerprintAvailable: Boolean,
        onDelay: (com.solthariel.pawchan.domain.security.AppLockDelay) -> Unit,
        onEdit: (LockAction) -> Unit,
    ) -> Unit,
) {
    val model = LocalAppLock.current ?: return
    val state by model.state.collectAsStateWithLifecycle()
    val activity = LocalActivity.current ?: return
    content(
        state.settingsProjection(),
        model.fingerprintAvailable,
        model::setDelay,
        model::edit,
    )
    LaunchedEffect(state.biometricSetupReady) {
        if (state.biometricSetupReady) model.biometric(activity, setup = true)
    }
    if (state.editor != null) LockEditorDialog(model, state, activity)
}

@Composable
private fun LockEditorDialog(
    model: AppLockSession,
    state: AppLockState,
    activity: Activity,
) {
    val editor = state.editor ?: return
    AppDialog(model::cancelEdit) {
        Column(Modifier.verticalScroll(rememberScrollState()), horizontalAlignment = Alignment.CenterHorizontally) {
            val title =
                when (editor.stage) {
                    PinStage.CURRENT, PinStage.BIOMETRIC -> "Введите PIN-код"
                    PinStage.NEW -> "Новый PIN-код"
                    PinStage.CONFIRM -> "Повторите PIN-код"
                }
            PinPad(title, state, model::enterPin)
            if (state.biometricSetupReady) {
                TextButton(onClick = { model.biometric(activity, setup = true) }) { Text("Подтвердить отпечаток") }
            }
            TextButton(enabled = !state.busy, onClick = model::cancelEdit) { Text("Отмена") }
        }
    }
}
