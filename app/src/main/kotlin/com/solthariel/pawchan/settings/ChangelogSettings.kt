package com.solthariel.pawchan.settings

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException

@Composable
internal fun ChangelogSettings() {
    val context = LocalContext.current
    var open by remember { mutableStateOf(false) }
    var sections by remember { mutableStateOf<List<ChangelogSection>?>(null) }
    var failed by remember { mutableStateOf(false) }
    LaunchedEffect(open) {
        if (open && sections == null) {
            failed = false
            sections =
                withContext(Dispatchers.IO) {
                    try {
                        context.assets
                            .open("settings-changelog.txt")
                            .bufferedReader()
                            .use { parseChangelog(it.readText()) }
                    } catch (_: IOException) {
                        null
                    }
                }
            failed = sections == null
        }
    }
    SettingsItemRow(SettingsItem.Action("Список изменений", null, onClick = { open = true }))
    if (open) ChangelogDialog(sections, failed) { open = false }
}
