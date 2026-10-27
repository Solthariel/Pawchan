package com.solthariel.pawchan.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.solthariel.pawchan.domain.network.MirrorAvailability
import com.solthariel.pawchan.domain.settings.AppSettings
import com.solthariel.pawchan.infrastructure.LocalForumMirrors
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
internal fun MirrorAvailabilitySetting(
    app: AppSettings,
    update: ((AppSettings) -> AppSettings) -> Unit,
) {
    FilterCheck("Автоматически переключать зеркало", app.behavior.client.connection.automaticFallback, { enabled ->
        update { it.withClient { c -> c.copy(connection = c.connection.copy(automaticFallback = enabled)) } }
    })
    val mirrors = LocalForumMirrors.current ?: return
    val state by mirrors.state.collectAsStateWithLifecycle()
    val captions by produceState(emptyList<String>(), state.observations) {
        value =
            withContext(Dispatchers.Default) {
                state.observations.map { observation ->
                    val checked =
                        java.time.format.DateTimeFormatter
                            .ofPattern("HH:mm:ss")
                            .withZone(java.time.ZoneId.systemDefault())
                            .format(java.time.Instant.ofEpochMilli(observation.checkedAtMillis))
                    "${observation.host}: ${observation.availability.mirrorCaption()} — $checked"
                }
            }
    }
    Column(Modifier.padding(horizontal = 16.dp)) {
        Text("Текущее зеркало: ${state.activeHost}")
        TextButton(onClick = mirrors::checkAvailability, enabled = !state.checking) {
            Text(if (state.checking) "Проверка…" else "Проверить доступность")
        }
        captions.forEach { Text(it) }
        Text("Доступность с этого устройства. Проверка не переносит авторизацию между доменами.")
    }
}

private fun MirrorAvailability.mirrorCaption(): String =
    when (this) {
        MirrorAvailability.AVAILABLE -> "Доступно"
        MirrorAvailability.TIMEOUT -> "Нет ответа вовремя"
        MirrorAvailability.SERVER_ERROR -> "Ошибка сервера"
        MirrorAvailability.VERIFICATION -> "Нужна проверка или авторизация"
        MirrorAvailability.RATE_LIMITED -> "Слишком много запросов"
        MirrorAvailability.INVALID_RESPONSE -> "Некорректный ответ"
        MirrorAvailability.NETWORK_ERROR -> "Ошибка сети"
    }
