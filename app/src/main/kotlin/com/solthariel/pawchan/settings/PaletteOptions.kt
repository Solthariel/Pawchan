package com.solthariel.pawchan.settings

import android.os.Build
import com.solthariel.pawchan.domain.settings.ThemePalette
import com.solthariel.pawchan.domain.settings.currentPalette

internal fun availableThemePalettes(sdk: Int): List<ThemePalette> =
    ThemePalette.entries.filter {
        it.currentPalette() == it && (it != ThemePalette.SYSTEM || sdk >= Build.VERSION_CODES.S)
    }

internal fun ThemePalette.paletteLabel(): String =
    when (this) {
        ThemePalette.SYSTEM -> "Из обоев устройства"
        ThemePalette.LAVENDER -> "Лавандовая"
        ThemePalette.BLUE -> "Васильковая"
        ThemePalette.OLIVE -> "Ванильная"
        ThemePalette.PEACH -> "Персиковая"
        ThemePalette.ROSE -> "Коралловая"
        ThemePalette.BURGUNDY -> "Коралловая"
        ThemePalette.AMBER -> "Ванильная"
        ThemePalette.GREEN -> "Мятная"
        ThemePalette.TEAL -> "Бирюзовая"
        ThemePalette.GRAY -> "Галечная"
        ThemePalette.CUSTOM -> "Свой цвет"
    }
