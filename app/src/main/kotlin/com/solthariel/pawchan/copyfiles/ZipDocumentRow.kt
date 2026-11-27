package com.solthariel.pawchan.copyfiles

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.solthariel.pawchan.catalog.CatalogIconButton
import com.solthariel.pawchan.core.ui.UiIconKind
import com.solthariel.pawchan.core.ui.UiMenuIcon
import com.solthariel.pawchan.core.ui.UiSpacing
import java.text.DateFormat
import java.util.Date
import com.solthariel.pawchan.R.string as L

@Composable
internal fun ZipDocumentRow(
    document: CopyDocument,
    onOpen: () -> Unit,
    onMore: (() -> Unit)?,
    available: Boolean = true,
) {
    val res = LocalResources.current

    val metadata = remember(document.modified, document.bytes, available) { zipMetadata(document, available) }
    Row(verticalAlignment = Alignment.CenterVertically) {
        Row(
            Modifier
                .weight(1f)
                .heightIn(min = 48.dp)
                .clickable(enabled = available, role = Role.Button, onClick = onOpen)
                .padding(vertical = UiSpacing.small),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            UiMenuIcon(UiIconKind.ARCHIVE)
            Spacer(Modifier.width(UiSpacing.medium))
            Column(Modifier.weight(1f)) {
                Text(
                    zipDisplayName(document.name),
                    Modifier.fillMaxWidth().semantics { contentDescription = document.name },
                    style = MaterialTheme.typography.bodyLarge,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    metadata,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        if (onMore != null) CatalogIconButton(UiIconKind.MORE, res.getString(L.ui_file_actions), onClick = onMore)
    }
}

/** Keep both the distinctive beginning and the extension without splitting Unicode characters. */
internal fun zipDisplayName(name: String): String {
    val length = name.codePointCount(0, name.length)
    if (length <= MAX_NAME_CODE_POINTS) return name
    val start = name.offsetByCodePoints(0, NAME_PREFIX_CODE_POINTS)
    val end = name.offsetByCodePoints(0, length - NAME_SUFFIX_CODE_POINTS)
    return name.substring(0, start) + "…" + name.substring(end)
}

private fun zipMetadata(
    document: CopyDocument,
    available: Boolean,
): String {
    if (!available) return "Недоступен"
    val date =
        if (document.modified > 0) {
            DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT).format(Date(document.modified))
        } else {
            "Дата неизвестна"
        }
    val size = if (document.bytes >= 0) "${document.bytes / BYTES_PER_KIB} КБ" else "Размер неизвестен"
    return "$date · $size"
}

private const val MAX_NAME_CODE_POINTS = 34
private const val NAME_PREFIX_CODE_POINTS = 22
private const val NAME_SUFFIX_CODE_POINTS = 11
private const val BYTES_PER_KIB = 1024
