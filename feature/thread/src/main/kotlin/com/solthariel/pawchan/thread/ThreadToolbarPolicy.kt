package com.solthariel.pawchan.thread

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.solthariel.pawchan.core.ui.APP_TOOLBAR_BUTTON_SIZE
import com.solthariel.pawchan.core.ui.APP_TOOLBAR_HORIZONTAL_INSET

/** Six full-size actions and a visible separation between navigation and thread actions. */
internal fun threadToolbarUsesCompactActions(availableWidth: Dp): Boolean =
    availableWidth < APP_TOOLBAR_BUTTON_SIZE * FULL_ACTION_COUNT + APP_TOOLBAR_HORIZONTAL_INSET * 2 + 16.dp

private const val FULL_ACTION_COUNT = 6
