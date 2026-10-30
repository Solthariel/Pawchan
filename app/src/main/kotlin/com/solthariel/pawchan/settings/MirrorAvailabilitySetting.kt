package com.solthariel.pawchan.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.solthariel.pawchan.core.ui.AppAlertDialog
import com.solthariel.pawchan.domain.settings.AppSettings
import com.solthariel.pawchan.infrastructure.LocalForumMirrors
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
internal fun MirrorAvailabilitySetting(
    app: AppSettings,
    update: ((AppSettings) -> AppSettings) -> Unit,
) {
    HorizontalDivider(Modifier.padding(horizontal = 16.dp))
    SettingsItemRow(
        SettingsItem.Toggle(
            "Автоматически переключать зеркало",
            "Если выбранное зеркало недоступно",
            app.behavior.client.connection.automaticFallback,
        ) { enabled ->
            update { it.withClient { c -> c.copy(connection = c.connection.copy(automaticFallback = enabled)) } }
        },
    )
    val mirrors = LocalForumMirrors.current ?: return
    val state by mirrors.state.collectAsStateWithLifecycle()
    val presentation by produceState(MirrorAvailabilityPresentation(), state.observations) {
        value = withContext(Dispatchers.Default) { mirrorAvailabilityPresentation(state.observations) }
    }
    HorizontalDivider(Modifier.padding(horizontal = 16.dp))
    MirrorAvailabilityResults(presentation, state.checking, mirrors::checkAvailability)
    MirrorAvailabilityHelp()
}

@Composable
private fun MirrorAvailabilityHelp() {
    var open by remember { mutableStateOf(false) }
    SettingsItemRow(SettingsItem.Action("Как работает переключение", null, onClick = { open = true }))
    if (open) {
        AppAlertDialog(
            onDismissRequest = { open = false },
            title = { Text("Переключение зеркал") },
            text = {
                Column {
                    Text("Проверяется доступность с этого устройства, а не для всех пользователей.")
                    Text(
                        "Время HTTP-проверки включает соединение, получение и проверку ответа. " +
                            "Это не пинг и не скорость загрузки медиа.",
                    )
                    Text("Автоматический выбор действует до перезапуска приложения и не меняет выбранный домен.")
                    Text("Авторизация не переносится между доменами. Ошибки капчи и ограничения запросов не обходятся.")
                }
            },
            confirmButton = {
                androidx.compose.material3.TextButton(onClick = { open = false }) { Text("Закрыть") }
            },
        )
    }
}
