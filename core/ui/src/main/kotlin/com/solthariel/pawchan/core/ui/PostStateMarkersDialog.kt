package com.solthariel.pawchan.core.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.solthariel.pawchan.core.ui.AppDialog

/** Список значков поста в стиле меню файла: без кнопки ОК, закрывается тапом мимо или «Назад». */
@Composable
internal fun PostStateMarkersDialog(
    markers: List<PostStateMarker>,
    email: String?,
    onClose: () -> Unit,
) {
    val context = LocalContext.current
    AppDialog(onClose) {
        Column(Modifier.verticalScroll(rememberScrollState()).padding(8.dp)) {
            markers.forEach { marker ->
                val address = email?.takeIf { marker == PostStateMarker.EMAIL }
                MarkerRow(marker, address) {
                    address?.let {
                        copyTextToClipboard(context, "Email", it)
                        onClose()
                    }
                }
            }
        }
    }
}

@Composable
private fun MarkerRow(
    marker: PostStateMarker,
    address: String?,
    onClick: () -> Unit,
) {
    Row(
        Modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp)
            .clip(RoundedCornerShape(8.dp))
            .clickable(enabled = address != null, onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        UiIcon(marker.icon, marker.color(), Modifier.size(20.dp))
        Column(Modifier.weight(1f)) {
            Text(stringResource(marker.label), style = MaterialTheme.typography.bodyLarge)
            address?.let {
                Text(
                    it,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}
