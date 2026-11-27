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
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.unit.dp
import com.solthariel.pawchan.core.ui.postContainerShape
import com.solthariel.pawchan.feature.thread.R.string as L

@Composable
internal fun HiddenPostCard(
    post: ThreadPostUi,
    onShow: () -> Unit,
    onMenu: () -> Unit,
) {
    val res = LocalResources.current

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
                    res.getString(L.ui_hidden_by_filter_tap_to, post.metadata.contentFilterLabel)
                } else {
                    res.getString(L.ui_post_hidden_tap_to_show, post.id)
                },
                Modifier.padding(12.dp),
                style = MaterialTheme.typography.bodySmall,
            )
            val edit = LocalEditContentFilter.current
            val ruleId = post.metadata.contentFilterId
            if (edit != null && ruleId != null) {
                TextButton(onClick = { edit(ruleId) }) { Text(res.getString(L.ui_rule)) }
            }
        }
    }
}
