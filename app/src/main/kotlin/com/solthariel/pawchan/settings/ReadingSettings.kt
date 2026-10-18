package com.solthariel.pawchan.settings

import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import com.solthariel.pawchan.domain.library.HistoryCleanup
import com.solthariel.pawchan.domain.model.DomainOutcome
import com.solthariel.pawchan.domain.settings.AppSettings
import com.solthariel.pawchan.domain.settings.ReadingPreferences
import com.solthariel.pawchan.workspace.LocalWorkspace
import kotlinx.coroutines.launch
import com.solthariel.pawchan.core.ui.AppAlertDialog as AlertDialog

@Composable
internal fun readingGroups(
    app: AppSettings,
    update: ((AppSettings) -> AppSettings) -> Unit,
): List<SettingsGroup> {
    val value = app.behavior.client.reading
    val change: ((ReadingPreferences) -> ReadingPreferences) -> Unit = { transform ->
        update { current -> current.withClient { it.copy(reading = transform(it.reading)) } }
    }
    return listOf(
        SettingsGroup(
            "Чтение и история",
            listOf(
                SettingsItem.Toggle(
                    "Восстанавливать последний экран",
                    "При новом запуске; открытые страницы сохраняются",
                    value.restoreLastScreen,
                ) { on ->
                    change {
                        it.copy(restoreLastScreen = on)
                    }
                },
                SettingsItem.Toggle(
                    "Запоминать позицию чтения",
                    "Выключение не мешает переходу к посту по ссылке",
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
