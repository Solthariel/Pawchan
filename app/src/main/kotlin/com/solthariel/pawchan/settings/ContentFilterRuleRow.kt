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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.solthariel.pawchan.domain.filter.ContentFilterRule

@Composable
internal fun ContentFilterRuleRow(
    rule: ContentFilterRule,
    count: Long? = null,
    edit: () -> Unit,
) {
    Row(
        Modifier.fillMaxWidth().clickable(onClick = edit).padding(horizontal = 16.dp, vertical = 12.dp),
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
    }
}

private fun ContentFilterRule.filterDescription(): String {
    val targets = listOfNotNull("ОП".takeIf { threads }, "Ответы".takeIf { comments }).joinToString(" + ")
    val boards = boards.joinToString { "/$it/" }.ifEmpty { "Все доски" }
    val disabled = if (enabled) "" else " — Выключено"
    return "$boards — $targets — ${filterModeLabel(mode)}$disabled"
}

internal fun ContentFilterRule.matchesRuleSearch(query: String): Boolean {
    val matchesQuery =
        query.isBlank() || listOf(label, site, boards.joinToString()).any { it.contains(query, ignoreCase = true) }
    return matchesQuery
}

internal fun Long?.orZeroWhenAvailable(available: Boolean): Long? = if (available) this ?: 0L else null

@Composable
private fun FilterRuleCounter(count: Long?) {
    val shape = RoundedCornerShape(4.dp)
    Box(
        Modifier
            .padding(start = 12.dp)
            .heightIn(min = 24.dp)
            .widthIn(min = 24.dp)
            .background(MaterialTheme.colorScheme.surfaceVariant, shape)
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, shape)
            .padding(horizontal = 6.dp, vertical = 3.dp)
            .semantics { contentDescription = count?.let { "Срабатываний: $it" } ?: "Статистика загружается" },
        contentAlignment = Alignment.Center,
    ) {
        Text(count?.toString() ?: "—", style = MaterialTheme.typography.bodyMedium)
    }
}
