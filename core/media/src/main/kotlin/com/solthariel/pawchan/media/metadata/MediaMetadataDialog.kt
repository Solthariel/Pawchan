package com.solthariel.pawchan.media.metadata

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.solthariel.pawchan.core.media.R
import com.solthariel.pawchan.core.ui.AppDialog
import com.solthariel.pawchan.core.ui.AppDialogStyle
import com.solthariel.pawchan.core.ui.copyTextToClipboard

@Composable
internal fun MediaMetadataDialog(
    state: MediaMetadataState,
    onClose: () -> Unit,
) {
    AppDialog(onClose) {
        Column(
            Modifier.verticalScroll(rememberScrollState()).padding(AppDialogStyle.contentPadding),
        ) {
            Text(stringResource(R.string.media_menu_metadata), style = MaterialTheme.typography.titleLarge)
            val metadata = state.metadata
            if (metadata == null) {
                MetadataStatus(state, Modifier.padding(top = 12.dp))
            } else {
                metadata.sections.forEach { MetadataSectionView(it) }
            }
        }
    }
}

@Composable
private fun MetadataSectionView(section: MediaMetadataSection) {
    Text(
        section.title,
        Modifier.padding(top = 16.dp, bottom = 4.dp),
        color = MaterialTheme.colorScheme.primary,
        style = MaterialTheme.typography.labelLarge,
    )
    section.rows.forEach { MetadataRowView(it) }
}

/** Долгое нажатие копирует значение — удобно для кодировщика или названия. */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun MetadataRowView(row: MediaMetadataRow) {
    val context = LocalContext.current
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(4.dp))
            .combinedClickable(onClick = {}, onLongClick = { copyTextToClipboard(context, row.label, row.value) })
            .padding(vertical = 4.dp),
    ) {
        Text(
            row.label,
            Modifier.weight(LABEL_WEIGHT).padding(end = 12.dp),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.bodyMedium,
        )
        Text(row.value, Modifier.weight(1f - LABEL_WEIGHT), style = MaterialTheme.typography.bodyMedium)
    }
}

/**
 * Сводка под пунктами меню у ⋮; нажатие открывает полное окно. Пока сведения грузятся, место под строку
 * уже занято — меню не прыгает по высоте; если сведений нет, блока нет вовсе.
 */
@Composable
internal fun MediaMetadataSummary(
    state: MediaMetadataState,
    onClick: () -> Unit,
) {
    val summary = state.metadata?.summary.orEmpty()
    if (!state.loading && summary.isEmpty()) return
    HorizontalDivider(Modifier.padding(vertical = 4.dp))
    MediaSummaryText(
        summary,
        Modifier
            .fillMaxWidth()
            .clickable(enabled = !state.loading, onClick = onClick)
            .padding(horizontal = SUMMARY_PADDING, vertical = 10.dp),
    )
}

@Composable
private fun MetadataStatus(
    state: MediaMetadataState,
    modifier: Modifier = Modifier,
) {
    Text(
        stringResource(if (state.loading) R.string.media_metadata_loading else R.string.media_metadata_missing),
        modifier,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        style = MaterialTheme.typography.bodyMedium,
    )
}

private const val LABEL_WEIGHT = 0.4f

/** Как у текста пунктов меню (TextButton): строка стоит с ними на одной линии. */
private val SUMMARY_PADDING = 12.dp
