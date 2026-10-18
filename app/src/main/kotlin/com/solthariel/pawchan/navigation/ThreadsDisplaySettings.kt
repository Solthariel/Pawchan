package com.solthariel.pawchan.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import com.solthariel.pawchan.domain.settings.AppSettingsRepository
import com.solthariel.pawchan.domain.settings.NavigationPreferences
import kotlinx.coroutines.launch

/** Screen-owned persistence, separate from rendering and temporary dialog drafts. */
@Composable
internal fun rememberThreadLimitsUpdate(repository: AppSettingsRepository?): (NavigationPreferences) -> Unit {
    val scope = rememberCoroutineScope()
    return { limits ->
        scope.launch {
            repository?.updateAppSettings { current ->
                val client = current.behavior.client
                current.copy(
                    behavior =
                        current.behavior.copy(
                            client = client.copy(reading = client.reading.copy(navigation = limits.normalized())),
                        ),
                )
            }
        }
    }
}
