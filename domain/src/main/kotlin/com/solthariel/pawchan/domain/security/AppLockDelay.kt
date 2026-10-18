package com.solthariel.pawchan.domain.security

/** Explicit persisted durations; enum ordinals are never stored. */
enum class AppLockDelay(
    val millis: Long,
) {
    IMMEDIATE(0L),
    SECONDS_30(THIRTY_SECONDS_MILLIS),
    MINUTE_1(ONE_MINUTE_MILLIS),
    MINUTES_2(TWO_MINUTES_MILLIS),
    MINUTES_5(FIVE_MINUTES_MILLIS),
    MINUTES_30(THIRTY_MINUTES_MILLIS),
    HOUR_1(ONE_HOUR_MILLIS),
    ;

    companion object {
        fun fromMillis(value: Long): AppLockDelay = entries.firstOrNull { it.millis == value } ?: MINUTE_1
    }
}

private const val THIRTY_SECONDS_MILLIS = 30_000L
private const val ONE_MINUTE_MILLIS = 60_000L
private const val TWO_MINUTES_MILLIS = 120_000L
private const val FIVE_MINUTES_MILLIS = 300_000L
private const val THIRTY_MINUTES_MILLIS = 1_800_000L
private const val ONE_HOUR_MILLIS = 3_600_000L
