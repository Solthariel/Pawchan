package com.solthariel.pawchan.settings

import com.solthariel.pawchan.domain.network.MirrorAvailability
import com.solthariel.pawchan.domain.network.MirrorObservation
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

internal data class MirrorStatusRow(
    val host: String,
    val caption: String,
)

internal data class MirrorAvailabilityPresentation(
    val rows: List<MirrorStatusRow> = emptyList(),
    val checkedCaption: String? = null,
)

internal fun mirrorAvailabilityPresentation(
    observations: List<MirrorObservation>,
    zone: ZoneId = ZoneId.systemDefault(),
): MirrorAvailabilityPresentation {
    val latest = observations.maxOfOrNull { it.checkedAtMillis }
    val checked =
        latest?.let { DateTimeFormatter.ofPattern("HH:mm:ss").withZone(zone).format(Instant.ofEpochMilli(it)) }
    return MirrorAvailabilityPresentation(
        observations.map { MirrorStatusRow(it.host, it.availability.mirrorCaption()) },
        checked?.let { "Проверено с этого устройства в $it" },
    )
}

internal fun activeMirrorCaption(
    preferred: String,
    active: String,
): String? = active.takeIf { it != preferred }?.let { "Сейчас используется: $it" }

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
