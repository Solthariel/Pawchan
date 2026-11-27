package com.solthariel.pawchan.core.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.unit.dp
import com.solthariel.pawchan.core.ui.R.string as L

/** Shared footer layout for reply counts and the expand action. */
@Composable
fun PostFooterLayout(content: @Composable RowScope.() -> Unit) {
    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
        content = content,
    )
}

@Composable
fun RowScope.PostExpandAction(
    enabled: Boolean,
    onExpand: () -> Unit,
) {
    val res = LocalResources.current

    PostStatsText(
        res.getString(L.ui_expand),
        Modifier
            .weight(1f)
            .clickable(enabled = enabled, role = Role.Button, onClick = onExpand),
    )
}

fun postTextIsLimited(
    result: TextLayoutResult,
    expanded: Boolean,
    maxLines: Int,
): Boolean = !expanded && (result.hasVisualOverflow || result.lineCount > maxLines)
