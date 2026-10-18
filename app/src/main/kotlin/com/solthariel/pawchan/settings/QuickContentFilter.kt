package com.solthariel.pawchan.settings

import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.solthariel.pawchan.domain.filter.ContentFilterRule
import com.solthariel.pawchan.domain.filter.ContentFilterSample
import com.solthariel.pawchan.domain.model.DomainOutcome
import com.solthariel.pawchan.domain.settings.AppSettings
import com.solthariel.pawchan.domain.settings.AppSettingsRepository
import com.solthariel.pawchan.thread.ThreadContentFilterTools
import kotlinx.coroutines.launch
import com.solthariel.pawchan.core.ui.AppAlertDialog as AlertDialog

@Composable
internal fun rememberQuickContentFilter(repository: AppSettingsRepository?): ThreadContentFilterTools? {
    if (repository == null) return null
    var editing by remember { mutableStateOf<ContentFilterRule?>(null) }
    val app by repository.appSettings.collectAsStateWithLifecycle(
        com.solthariel.pawchan.domain.settings
            .AppSettings(),
    )
    return ThreadContentFilterTools(
        add = { selection ->
            editing =
                newContentFilterRule(selection.board, selection.text, selection.comment)
        },
        edit = { id -> editing = app.contentFilters.firstOrNull { it.id == id } },
        editor = { samples ->
            editing?.let { rule ->
                val existing = app.contentFilters.any { it.id == rule.id }
                QuickFilterEditor(repository, rule, samples, existing) { editing = null }
            }
        },
    )
}

@Composable
private fun QuickFilterEditor(
    repository: AppSettingsRepository,
    rule: ContentFilterRule,
    samples: List<ContentFilterSample>,
    existing: Boolean,
    close: () -> Unit,
) {
    var failure by remember { mutableStateOf(false) }
    var saving by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val commit: ((AppSettings) -> AppSettings) -> Unit =
        { transform ->
            if (!saving) {
                scope.launch {
                    saving = true
                    val outcome = repository.updateAppSettings(transform)
                    saving = false
                    if (outcome is DomainOutcome.Success) close() else failure = true
                }
            }
        }
    ContentFilterEditor(
        rule = rule,
        editorKey = rule.id,
        onClose = close,
        onSave = { next ->
            commit { app ->
                app.withContentFilters(
                    if (app.contentFilters.any { it.id == next.id }) {
                        app.contentFilters.map { if (it.id == next.id) next else it }
                    } else {
                        app.contentFilters + next
                    },
                )
            }
        },
        samples = samples,
        onDelete =
            if (existing) {
                { commit { app -> app.withContentFilters(app.contentFilters.filterNot { it.id == rule.id }) } }
            } else {
                null
            },
    )
    if (failure) {
        AlertDialog(
            onDismissRequest = { failure = false },
            title = { Text("Не удалось сохранить правило") },
            confirmButton = { TextButton(onClick = { failure = false }) { Text("Закрыть") } },
        )
    }
}
