package com.solthariel.pawchan.core.ui

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp

/** A divider belongs between list items, never inside a post or at a preview's outer edges. */
@Composable
fun PostSeparator(
    index: Int,
    lastIndex: Int,
) {
    if (!LocalPostDisplayPreferences.current.bubblePosts && index < lastIndex) {
        HorizontalDivider(
            Modifier.padding(horizontal = 8.dp).testTag("post-list-divider"),
            color = MaterialTheme.colorScheme.outlineVariant,
        )
    }
}
