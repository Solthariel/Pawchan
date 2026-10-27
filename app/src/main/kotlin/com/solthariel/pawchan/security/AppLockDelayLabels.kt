package com.solthariel.pawchan.security

import com.solthariel.pawchan.domain.security.AppLockDelay

internal val AppLockDelay.label: String
    get() =
        when (this) {
            AppLockDelay.IMMEDIATE -> "Сразу"
            AppLockDelay.SECONDS_30 -> "Через 30 секунд"
            AppLockDelay.MINUTE_1 -> "Через 1 минуту"
            AppLockDelay.MINUTES_2 -> "Через 2 минуты"
            AppLockDelay.MINUTES_5 -> "Через 5 минут"
            AppLockDelay.MINUTES_30 -> "Через 30 минут"
            AppLockDelay.HOUR_1 -> "Через 1 час"
            AppLockDelay.MINUTES_15 -> "Через 15 минут"
            AppLockDelay.HOURS_3 -> "Через 3 часа"
            AppLockDelay.HOURS_6 -> "Через 6 часов"
        }
