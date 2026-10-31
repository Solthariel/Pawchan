package com.solthariel.pawchan.catalog

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.unit.dp
import com.solthariel.pawchan.R
import com.solthariel.pawchan.core.ui.PostStatsText

@Composable
internal fun CatalogCardMetadata(thread: CatalogThreadUi) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Row(
            Modifier.weight(1f),
            horizontalArrangement = Arrangement.spacedBy(5.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            val display = thread.statistics
            CatalogMetadataCount(
                display.repliesText,
                display.repliesDescription,
                R.drawable.ic_catalog_replies,
                Modifier,
            )
            CatalogMetadataCount(
                display.filesText,
                display.filesDescription,
                R.drawable.ic_catalog_files,
                Modifier,
            )
            if (display.sizeText.isNotEmpty()) PostStatsText(display.sizeText, Modifier.weight(1f))
        }
        CatalogThreadStatusIcons(
            pinned = thread.pinned,
            closed = thread.closed,
            endless = thread.endless,
            modifier = Modifier.padding(start = 4.dp),
        )
        if (thread.age.text.isNotEmpty()) {
            PostStatsText(thread.age.text, Modifier.padding(start = 6.dp).widthIn(max = 100.dp))
        }
    }
}

@Composable
private fun CatalogMetadataCount(
    value: String,
    description: String,
    drawable: Int,
    modifier: Modifier,
) {
    Row(
        modifier.clearAndSetSemantics { contentDescription = description },
        horizontalArrangement = Arrangement.spacedBy(3.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            painterResource(drawable),
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(14.dp),
        )
        PostStatsText(value)
    }
}
