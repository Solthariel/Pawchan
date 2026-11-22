package com.solthariel.pawchan.core.ui

import androidx.compose.runtime.staticCompositionLocalOf

/** Only used by UI that intentionally sets its own line height, such as post text. */
val LocalTextLeadingMultiplier = staticCompositionLocalOf { 1f }
