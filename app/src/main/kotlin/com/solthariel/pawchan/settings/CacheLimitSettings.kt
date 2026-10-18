package com.solthariel.pawchan.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.solthariel.pawchan.core.ui.formatByteSizeForUi
import com.solthariel.pawchan.domain.settings.AppSettings
import com.solthariel.pawchan.domain.settings.StoragePreferences
import kotlin.math.roundToInt

internal fun cacheLimitItem(
    app: AppSettings,
    update: ((AppSettings) -> AppSettings) -> Unit,
): SettingsItem =
    SettingsItem.Custom("Лимит кэша") {
        CacheLimitSetting(app.behavior.client.storage.cacheMb) { size ->
            update { current -> current.withClient { it.copy(storage = it.storage.copy(cacheMb = size)) } }
        }
    }

@Composable
private fun CacheLimitSetting(
    size: Int,
    save: (Int) -> Unit,
) {
    var value by remember(size) { mutableIntStateOf(size) }
    Column(Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
        Row(Modifier.fillMaxWidth()) {
            Text("Лимит кэша", Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge)
            Text(
                formatByteSizeForUi(value.toLong() * BYTES_PER_MB),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodyMedium,
            )
        }
        Slider(
            value = cacheSliderPosition(value),
            onValueChange = { value = cacheSliderSize(it) },
            onValueChangeFinished = { save(value) },
            steps = CACHE_STEPS - 1,
            valueRange = 0f..1f,
        )
        Text(
            "Применяется после перезапуска",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

internal fun cacheSliderSize(position: Float): Int =
    (position.coerceIn(0f, 1f) * CACHE_STEPS)
        .roundToInt()
        .times(CACHE_STEP_MB)
        .coerceIn(StoragePreferences.MIN_MB, StoragePreferences.MAX_MB)

internal fun cacheSliderPosition(size: Int): Float =
    size.coerceIn(StoragePreferences.MIN_MB, StoragePreferences.MAX_MB).toFloat() / StoragePreferences.MAX_MB

private const val BYTES_PER_MB = 1_000_000L
private const val CACHE_STEP_MB = 100
private const val CACHE_STEPS = 100
