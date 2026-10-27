package com.solthariel.pawchan.settings

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import com.solthariel.pawchan.domain.filter.ContentFilterRule
import com.solthariel.pawchan.domain.filter.ContentFilterSample
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
internal fun ContentFilterEditor(
    rule: ContentFilterRule,
    editorKey: String,
    onClose: () -> Unit,
    onSave: (ContentFilterRule) -> Unit,
    samples: List<ContentFilterSample> = emptyList(),
    onDelete: (() -> Unit)? = null,
) {
    val draft = rememberSaveable(editorKey, saver = ContentFilterDraft.Saver) { ContentFilterDraft(rule) }
    val next = draft.rule(rule)
    var probeText by rememberSaveable(editorKey) { mutableStateOf(initialFilterProbeText(rule, samples)) }
    val validation = remember(editorKey) { ContentFilterValidationState() }
    val scope = rememberCoroutineScope()
    LaunchedEffect(next) {
        delay(FILTER_VALIDATION_DELAY_MILLIS)
        validation.check(next)
    }
    val error = draft.inputError() ?: validation.errorFor(next)
    FilterEditorWindow(
        draft = draft,
        onClose = onClose,
        onDelete = onDelete,
        confirm = {
            TextButton(enabled = validation.canSave(draft), onClick = {
                scope.launch {
                    validation.save(draft.rule(rule), draft.inputError(), { draft.rule(rule) }, onSave)
                }
            }) {
                Text("Сохранить")
            }
        },
    ) {
        FilterEditorFields(draft, probeText)
        ContentFilterProbe(next, samples, probeText) { probeText = it }
        FilterLegacyFields(draft)
        if (error != null && !filterEmptyErrorIsInline(error, draft.text)) {
            Text(error, color = MaterialTheme.colorScheme.error)
        }
    }
}

private const val FILTER_VALIDATION_DELAY_MILLIS = 250L
