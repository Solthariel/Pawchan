package com.solthariel.pawchan.ui

import com.solthariel.pawchan.domain.settings.DEFAULT_THEME_SEED
import com.solthariel.pawchan.domain.settings.ThemePalette

/** Source colors, not final UI role values. Persisted enum keys remain stable across preset revisions. */
internal fun ThemePalette.seedColor(): Int =
    when (this) {
        ThemePalette.BLUE -> BLUE_SEED
        ThemePalette.OLIVE -> AMBER_SEED
        ThemePalette.PEACH -> PEACH_SEED
        ThemePalette.ROSE -> BURGUNDY_SEED
        ThemePalette.BURGUNDY -> BURGUNDY_SEED
        ThemePalette.AMBER -> AMBER_SEED
        ThemePalette.GREEN -> GREEN_SEED
        ThemePalette.TEAL -> TEAL_SEED
        ThemePalette.GRAY -> GRAY_SEED
        else -> DEFAULT_THEME_SEED
    }

private const val BLUE_SEED = 0x7386C8
private const val PEACH_SEED = 0xB85C26
private const val BURGUNDY_SEED = 0xF94B4D
private const val AMBER_SEED = 0xF3E5AB
private const val GREEN_SEED = 0xC7F5D5
private const val TEAL_SEED = 0xB9EBEA
private const val GRAY_SEED = 0x929398
