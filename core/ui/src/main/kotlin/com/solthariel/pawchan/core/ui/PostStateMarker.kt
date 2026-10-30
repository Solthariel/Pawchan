package com.solthariel.pawchan.core.ui

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** Значки состояния поста в порядке отображения; «мой пост» показывается полосой карточки. */
enum class PostStateMarker(
    val icon: UiIconKind,
    @param:StringRes val label: Int,
) {
    ORIGINAL_POSTER(UiIconKind.PERSON, R.string.core_ui_thread_author),
    SAGE(UiIconKind.SAGE, R.string.core_ui_post_sage),
    EMAIL(UiIconKind.MAIL, R.string.core_ui_post_email),
    STICKY(UiIconKind.PIN, R.string.core_ui_thread_sticky),
    CLOSED(UiIconKind.LOCK, R.string.core_ui_thread_closed),
    CYCLICAL(UiIconKind.INFINITY, R.string.core_ui_thread_cyclical),
    BANNED(UiIconKind.BANNED, R.string.core_ui_post_banned),
}

/**
 * Значки 12sp рядом с датой. Вся группа — одна цель нажатия: тап открывает список значков поста
 * с подписями (почта — с адресом, тап копирует).
 */
@Composable
fun PostStateMarkers(
    markers: List<PostStateMarker>,
    email: String? = null,
) {
    if (markers.isEmpty()) return
    var open by remember { mutableStateOf(false) }
    // Размер привязан к шрифту строки статуса (12sp): иконка не выше цифр и масштабируется вместе с текстом.
    val iconSize = with(LocalDensity.current) { MARKER_SIZE.toDp() }
    val description = markers.map { stringResource(it.label) }.joinToString()
    TextButton(
        onClick = { open = true },
        modifier =
            Modifier
                .heightIn(min = 48.dp)
                .widthIn(min = 48.dp)
                .semantics { contentDescription = description },
        contentPadding = PaddingValues(4.dp),
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            markers.forEach { marker -> UiIcon(marker.icon, marker.color(), Modifier.size(iconSize)) }
        }
    }
    if (open) PostStateMarkersDialog(markers, email) { open = false }
}

@Composable
internal fun PostStateMarker.color(): Color {
    val colors = MaterialTheme.colorScheme
    return when (this) {
        PostStateMarker.ORIGINAL_POSTER -> colors.primary
        PostStateMarker.SAGE -> colors.tertiary
        PostStateMarker.BANNED -> colors.error
        else -> colors.onSurfaceVariant
    }
}

private val MARKER_SIZE = 12.sp
