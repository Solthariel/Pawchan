package com.solthariel.pawchan.settings

import com.solthariel.pawchan.domain.network.MirrorAvailability
import com.solthariel.pawchan.domain.network.MirrorObservation
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

internal data class MirrorStatusRow(
    val host: String,
    val caption: String,
    val responseCaption: String? = null,
)

internal data class MirrorAvailabilityPresentation(
    val rows: List<MirrorStatusRow> = emptyList(),
    val checkedCaption: String? = null,
    val summary: String? = null,
)

internal fun mirrorAvailabilityPresentation(
    observations: List<MirrorObservation>,
    zone: ZoneId = ZoneId.systemDefault(),
): MirrorAvailabilityPresentation {
    val latest = observations.maxOfOrNull { it.checkedAtMillis }
    val checked =
        latest?.let { DateTimeFormatter.ofPattern("HH:mm:ss").withZone(zone).format(Instant.ofEpochMilli(it)) }
    return MirrorAvailabilityPresentation(
        observations.map {
            val successfulSamples = it.successfulSamples
            val totalSamples = it.totalSamples
            MirrorStatusRow(
                it.host,
                if (successfulSamples != null && totalSamples != null &&
                    successfulSamples in 1 until totalSamples
                ) {
                    "Нестабильно"
                } else {
                    it.availability.mirrorCaption()
                },
                if (totalSamples != null && successfulSamples != null) {
                    listOfNotNull(
                        it.responseTimeMillis?.let { time -> "Среднее: $time мс" },
                        "Ответов: $successfulSamples из $totalSamples",
                    ).joinToString(", ")
                } else {
                    it.responseTimeMillis?.takeIf { time -> time >= 0 }?.let { time -> "HTTP-проверка: $time мс" }
                },
            )
        },
        checked?.let { "Проверено с этого устройства в $it" },
        checked?.let {
            "Доступны ${observations.count { row -> row.availability == MirrorAvailability.AVAILABLE }} " +
                "из ${observations.size} — проверено в $it"
        },
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
