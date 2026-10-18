package com.solthariel.pawchan.media.metadata

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import com.solthariel.pawchan.core.ui.formatByteSizeForUi

/** Known fields while richer metadata is loading or unavailable. */
fun basicMediaSummary(
    format: String?,
    resolution: String?,
    bytes: Long,
): String =
    listOfNotNull(format, resolution, bytes.takeIf { it > 0 }?.let(::formatByteSizeForUi))
        .joinToString(SUMMARY_SEPARATOR)

/** Attachment editor and viewer use the same metadata formatting and typography. */
@Composable
fun MediaFileSummary(
    source: MediaMetadataSource,
    fallback: String,
    modifier: Modifier = Modifier,
) {
    val state = rememberMediaMetadata(source)
    val summary = state.metadata?.summary?.takeIf(String::isNotBlank) ?: fallback
    MediaSummaryText(summary, modifier, maxLines = 2)
}

@Composable
internal fun MediaSummaryText(
    summary: String,
    modifier: Modifier = Modifier,
    maxLines: Int = 1,
) {
    Text(
        summary,
        modifier,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        style = MaterialTheme.typography.bodySmall,
        maxLines = maxLines,
        overflow = TextOverflow.Ellipsis,
    )
}
