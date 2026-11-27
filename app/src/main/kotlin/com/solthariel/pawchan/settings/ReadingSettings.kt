package com.solthariel.pawchan.settings

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalResources
import com.solthariel.pawchan.domain.settings.AppSettings
import com.solthariel.pawchan.domain.settings.ReadingPreferences
import com.solthariel.pawchan.R.string as L

@Composable
internal fun readingGroups(
    app: AppSettings,
    update: ((AppSettings) -> AppSettings) -> Unit,
): List<SettingsGroup> {
    val res = LocalResources.current

    val value = app.behavior.client.reading
    val change: ((ReadingPreferences) -> ReadingPreferences) -> Unit = { transform ->
        update { current -> current.withClient { it.copy(reading = transform(it.reading)) } }
    }
    return listOf(
        SettingsGroup(
            res.getString(L.ui_reading_and_history),
            listOf(
                SettingsItem.Toggle(
                    res.getString(L.ui_restore_last_screen),
                    res.getString(L.ui_on_startup),
                    value.restoreLastScreen,
                ) { on ->
                    change {
                        it.copy(restoreLastScreen = on)
                    }
                },
                SettingsItem.Toggle(
                    res.getString(L.ui_remember_reading_position),
                    res.getString(L.ui_for_each_thread),
                    value.rememberPosition,
                ) { on ->
                    change {
                        it.copy(rememberPosition = on)
                    }
                },
            ),
        ),
    )
}
