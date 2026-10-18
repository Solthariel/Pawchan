package com.solthariel.pawchan.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.solthariel.pawchan.core.ui.FAST_SCROLL_CONTENT_INSET
import com.solthariel.pawchan.domain.filter.ContentFilterRule
import com.solthariel.pawchan.domain.filter.ContentFilterStatistics

@Composable
internal fun ContentFilterRulesList(
    rules: List<ContentFilterRule>,
    statistics: ContentFilterStatistics,
    emptyMessage: String,
    modifier: Modifier,
    edit: (String) -> Unit,
) {
    Column(modifier.fillMaxWidth().verticalScroll(rememberScrollState())) {
        SettingsGroupFrame(horizontalInset = FAST_SCROLL_CONTENT_INSET) {
            if (rules.isEmpty()) {
                Text(
                    emptyMessage,
                    Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                    style = MaterialTheme.typography.bodyLarge,
                )
            }
            rules.forEachIndexed { index, rule ->
                if (index > 0) HorizontalDivider(Modifier.padding(horizontal = 16.dp))
                val count =
                    statistics.rules[rule.id].orZeroWhenAvailable(statistics.available && !statistics.storageFailure)
                ContentFilterRuleRow(rule, count) { edit(rule.id) }
            }
        }
    }
}
