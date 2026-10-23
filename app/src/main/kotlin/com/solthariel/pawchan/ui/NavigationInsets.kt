package com.solthariel.pawchan.ui

import androidx.compose.ui.unit.dp
import com.solthariel.pawchan.navigation.chrome.APP_BOTTOM_BAR_HEIGHT

/** App navigation chrome is not a dependency of the reusable refresh component. */
internal val APP_REFRESH_BOTTOM_INSET = APP_BOTTOM_BAR_HEIGHT + 6.dp
