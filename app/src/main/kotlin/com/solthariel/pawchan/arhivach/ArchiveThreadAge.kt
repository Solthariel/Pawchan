package com.solthariel.pawchan.arhivach

import com.solthariel.pawchan.catalog.currentEpochSeconds
import com.solthariel.pawchan.catalog.relativeThreadAge
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatterBuilder
import java.util.Locale

internal fun formatArchiveThreadAge(
    date: String?,
    nowEpochSeconds: Long = currentEpochSeconds(),
    zone: ZoneId = ZoneId.systemDefault(),
): String {
    val source = date?.trim().orEmpty()
    val timestamp =
        source
            .takeIf(String::isNotEmpty)
            ?.let {
                runCatching {
                    LocalDateTime
                        .parse(it, ARCHIVE_DATE_FORMAT)
                        .atZone(zone)
                        .toEpochSecond()
                }.getOrNull()
            }
    return timestamp?.let { relativeThreadAge(it, nowEpochSeconds) } ?: source
}

private val ARCHIVE_DATE_FORMAT =
    DateTimeFormatterBuilder()
        .appendPattern("uuuu-MM-dd HH:mm")
        .optionalStart()
        .appendPattern(":ss")
        .optionalEnd()
        .toFormatter(Locale.ROOT)
