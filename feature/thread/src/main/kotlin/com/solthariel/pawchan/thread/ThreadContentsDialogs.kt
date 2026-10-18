package com.solthariel.pawchan.thread

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.launch
import com.solthariel.pawchan.core.ui.AppAlertDialog as AlertDialog

/** «Скрытые посты»: правила скрытия этого треда, каждое можно убрать. */
@Composable
internal fun HiddenPostRulesDialog(
    preferences: ThreadPostPreferencesController,
    onClose: () -> Unit,
) {
    val rules by preferences.currentRules.collectAsStateWithLifecycle()
    val list = rules?.hiddenRules().orEmpty()
    val scope = rememberCoroutineScope()
    AlertDialog(
        onDismissRequest = onClose,
        title = { Text("Скрытые посты") },
        text = {
            if (list.isEmpty()) {
                Text("В этом треде ничего не скрыто")
            } else {
                Column(Modifier.heightIn(max = RULES_MAX_HEIGHT).verticalScroll(rememberScrollState())) {
                    list.forEach { rule ->
                        HiddenRuleRow(rule) { scope.launch { preferences.remove(rule) } }
                    }
                }
            }
        },
        confirmButton = {},
    )
}

@Composable
private fun HiddenRuleRow(
    rule: HiddenPostRule,
    onRemove: () -> Unit,
) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(
            rule.title,
            Modifier.weight(1f).padding(vertical = 8.dp),
            style = MaterialTheme.typography.bodyMedium,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
        TextButton(onClick = onRemove) { Text("Показать") }
    }
}

private val RULES_MAX_HEIGHT = 360.dp
