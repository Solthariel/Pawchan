package com.solthariel.pawchan.thread

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.solthariel.pawchan.core.ui.LocalTextLeadingMultiplier
import com.solthariel.pawchan.core.ui.PostExpandAction
import com.solthariel.pawchan.core.ui.PostFooterLayout
import com.solthariel.pawchan.core.ui.PostSubjectText

/** Shared body order for a post, regardless of its source. */
@Composable
fun PostTextContent(
    subject: String?,
    tag: String?,
    textSize: Int,
    content: @Composable () -> Unit,
) {
    Column {
        tag?.let {
            Text(
                "/$it/",
                Modifier.padding(bottom = TAG_GAP),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = (textSize + TAG_SIZE_EXTRA).sp,
                lineHeight =
                    (textSize + TAG_SIZE_EXTRA + POST_LINE_HEIGHT_EXTRA).sp * LocalTextLeadingMultiplier.current,
            )
        }
        subject?.let { PostSubjectText(it, textSize) }
        content()
    }
}

@Composable
fun PostFooterActions(
    replyLabel: String?,
    limited: Boolean,
    enabled: Boolean,
    onReplies: () -> Unit,
    onExpand: () -> Unit,
) {
    if (replyLabel == null && !limited) return
    PostFooterLayout {
        replyLabel?.let { label ->
            PostRepliesFooter(label, enabled, onReplies, if (limited) Modifier else Modifier.weight(1f))
        }
        if (limited) PostExpandAction(enabled, onExpand)
    }
}

private const val POST_LINE_HEIGHT_EXTRA = 5
private const val TAG_SIZE_EXTRA = 3
private val TAG_GAP = 6.dp
