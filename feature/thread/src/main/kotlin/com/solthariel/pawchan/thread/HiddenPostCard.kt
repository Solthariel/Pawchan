package com.solthariel.pawchan.thread

import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.solthariel.pawchan.core.ui.postContainerShape

@Composable
internal fun HiddenPostCard(
    post: ThreadPostUi,
    onShow: () -> Unit,
    onMenu: () -> Unit,
) {
    val shape = postContainerShape()
    Surface(
        modifier =
            Modifier
                .fillMaxWidth()
                .clip(shape)
                .combinedClickable(onClick = onShow, onLongClick = onMenu),
        color = MaterialTheme.colorScheme.surfaceVariant,
        shape = shape,
    ) {
        Column {
            Text(
                if (post.metadata.contentFilterLabel != null) {
                    "Скрыто фильтром: ${post.metadata.contentFilterLabel}  Нажмите, чтобы показать"
                } else {
                    "Пост #${post.id} скрыт  Нажмите, чтобы показать"
                },
                Modifier.padding(12.dp),
                style = MaterialTheme.typography.bodySmall,
            )
            val edit = LocalEditContentFilter.current
            val ruleId = post.metadata.contentFilterId
            if (edit != null && ruleId != null) {
                TextButton(onClick = { edit(ruleId) }) { Text("Правило") }
            }
        }
    }
}
