package com.solthariel.pawchan.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.solthariel.pawchan.domain.filter.ContentFilterRule
import com.solthariel.pawchan.domain.filter.isCurrentFilterMode
import com.solthariel.pawchan.R.string as L

@Composable
internal fun ContentFilterRuleRow(
    rule: ContentFilterRule,
    count: Long? = null,
    onEnabledChange: ((Boolean) -> Unit)? = null,
    edit: () -> Unit,
) {
    Row(
        Modifier
            .fillMaxWidth()
            .clickable(onClick = edit)
            .padding(
                start = 16.dp,
                top = 12.dp,
                bottom = 12.dp,
                end =
                    if (onEnabledChange !=
                        null
                    ) {
                        4.dp
                    } else {
                        16.dp
                    },
            ),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(
                rule.label,
                style = MaterialTheme.typography.bodyLarge,
                maxLines = 1,
                softWrap = false,
                overflow = TextOverflow.Ellipsis,
                color =
                    if (rule.enabled) {
                        MaterialTheme.colorScheme.onSurface
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
            )
            Text(
                rule.filterDescription(),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        FilterRuleCounter(count)
        if (onEnabledChange != null) FilterRuleEnabledCheckbox(rule, onEnabledChange)
    }
}

/** Отдельная цель нажатия 48 dp: переключает правило, не открывая редактор. */
@Composable
private fun FilterRuleEnabledCheckbox(
    rule: ContentFilterRule,
    onEnabledChange: (Boolean) -> Unit,
) {
    val res = LocalResources.current

    val description =
        if (rule.enabled) {
            res.getString(
                L.ui_disable_rule,
                rule.label,
            )
        } else {
            res.getString(L.ui_enable_rule, rule.label)
        }
    Box(
        Modifier
            .padding(start = 4.dp)
            .size(48.dp)
            .toggleable(rule.enabled, role = Role.Checkbox, onValueChange = onEnabledChange)
            .testTag("filter-rule-enabled-${rule.id}")
            .semantics { contentDescription = description },
        contentAlignment = Alignment.Center,
    ) {
        Checkbox(checked = rule.enabled, onCheckedChange = null)
    }
}

private fun ContentFilterRule.filterDescription(): String {
    val targets = listOfNotNull("ОП".takeIf { threads }, "Ответы".takeIf { comments }).joinToString(" + ")
    val boards = boards.joinToString { "/$it/" }.ifEmpty { "Все доски" }
    val disabled =
        if (!mode.isCurrentFilterMode) {
            " — Не применяется"
        } else if (enabled) {
            ""
        } else {
            " — Выключено"
        }
    return "$boards — $targets — ${filterModeLabel(mode)}$disabled"
}

internal fun Long?.orZeroWhenAvailable(available: Boolean): Long? = if (available) this ?: 0L else null

@Composable
private fun FilterRuleCounter(count: Long?) {
    val res = LocalResources.current

    val shape = RoundedCornerShape(4.dp)
    Box(
        Modifier
            .padding(start = 12.dp)
            .heightIn(min = 24.dp)
            .widthIn(min = 24.dp)
            .background(MaterialTheme.colorScheme.surfaceVariant, shape)
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, shape)
            .padding(horizontal = 6.dp, vertical = 4.dp)
            .semantics {
                contentDescription =
                    count?.let { res.getString(L.ui_triggers, it) }
                        ?: res.getString(L.ui_statistics_are_loading)
            },
        contentAlignment = Alignment.Center,
    ) {
        Text(count?.toString() ?: "—", style = MaterialTheme.typography.bodyMedium)
    }
}
