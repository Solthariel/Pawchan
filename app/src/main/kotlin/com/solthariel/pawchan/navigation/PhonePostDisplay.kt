package com.solthariel.pawchan.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.compose.LifecycleResumeEffect
import com.solthariel.pawchan.core.ui.PostDisplayPreferences
import com.solthariel.pawchan.domain.settings.AppSettings
import com.solthariel.pawchan.settings.toPostDisplay
import java.time.ZoneId

/** Refresh after returning from phone settings or travelling to another time zone. */
@Composable
internal fun rememberPhonePostDisplay(settings: AppSettings): PostDisplayPreferences {
    var zone by remember { mutableStateOf(ZoneId.systemDefault()) }
    LifecycleResumeEffect(Unit) {
        zone = ZoneId.systemDefault()
        onPauseOrDispose {}
    }
    return remember(settings, zone) { settings.toPostDisplay().copy(zone = zone) }
}
