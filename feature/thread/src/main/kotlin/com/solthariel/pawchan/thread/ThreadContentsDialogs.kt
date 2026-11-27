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
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.launch
import com.solthariel.pawchan.core.ui.AppAlertDialog as AlertDialog
import com.solthariel.pawchan.feature.thread.R.string as L

/** «Скрытые посты»: правила скрытия этого треда, каждое можно убрать. */
@Composable
internal fun HiddenPostRulesDialog(
    preferences: ThreadPostPreferencesController,
    onClose: () -> Unit,
) {
    val res = LocalResources.current

    val rules by preferences.currentRules.collectAsStateWithLifecycle()
    val list = rules?.hiddenRules().orEmpty()
    val scope = rememberCoroutineScope()
    AlertDialog(
        onDismissRequest = onClose,
        title = { Text(res.getString(L.ui_hidden_posts)) },
        text = {
            if (list.isEmpty()) {
                Text(res.getString(L.ui_nothing_is_hidden_in_this))
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
    val res = LocalResources.current

    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(
            rule.title,
            Modifier.weight(1f).padding(vertical = 8.dp),
            style = MaterialTheme.typography.bodyMedium,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
        TextButton(onClick = onRemove) { Text(res.getString(L.ui_show)) }
    }
}

private val RULES_MAX_HEIGHT = 360.dp
