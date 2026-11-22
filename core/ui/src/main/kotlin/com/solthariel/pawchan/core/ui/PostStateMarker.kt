package com.solthariel.pawchan.core.ui

import androidx.annotation.StringRes
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.TextUnit
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
    ARCHIVE_ACTIVE(UiIconKind.REFRESH, R.string.core_ui_archive_active),
    ARCHIVE_MAYBE_DELETED(UiIconKind.DELETE, R.string.core_ui_archive_maybe_deleted),
    ARCHIVE_SAVED(UiIconKind.ARCHIVE, R.string.core_ui_archive_saved),
    ARCHIVE_LOST(UiIconKind.WARNING, R.string.core_ui_archive_lost),
    ARCHIVE_QUEUED(UiIconKind.HISTORY, R.string.core_ui_archive_queued),
    ARCHIVE_OTHER(UiIconKind.INFO, R.string.core_ui_archive_other),
}

/**
 * Значки 12sp рядом с датой. Вся группа — одна цель нажатия: тап открывает список значков поста
 * с подписями (почта — с адресом, тап копирует).
 */
@Composable
fun PostStateMarkers(
    markers: List<PostStateMarker>,
    email: String? = null,
    labelOverrides: Map<PostStateMarker, String> = emptyMap(),
) {
    if (markers.isEmpty()) return
    var open by remember { mutableStateOf(false) }
    // Размер привязан к шрифту строки статуса (12sp): иконка не выше цифр и масштабируется вместе с текстом.
    val iconSize = with(LocalDensity.current) { POST_STATE_MARKER_SIZE.toDp() }
    val description = markers.map { labelOverrides[it] ?: stringResource(it.label) }.joinToString()
    Row(
        Modifier
            .clip(RoundedCornerShape(4.dp))
            .clickable(role = Role.Button) { open = true }
            .semantics { contentDescription = description },
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        markers.forEach { marker -> UiIcon(marker.icon, marker.color(), Modifier.size(iconSize)) }
    }
    if (open) PostStateMarkersDialog(markers, email, labelOverrides) { open = false }
}

@Composable
internal fun PostStateMarker.color(): Color {
    val colors = MaterialTheme.colorScheme
    return when (this) {
        PostStateMarker.ORIGINAL_POSTER, PostStateMarker.ARCHIVE_ACTIVE -> colors.primary
        PostStateMarker.SAGE, PostStateMarker.ARCHIVE_MAYBE_DELETED -> colors.tertiary
        PostStateMarker.ARCHIVE_SAVED -> colors.secondary
        PostStateMarker.BANNED, PostStateMarker.ARCHIVE_LOST -> colors.error
        else -> colors.onSurfaceVariant
    }
}

/** Размер значков состояния поста; маркер ссылки на ОП в тексте использует тот же размер. */
val POST_STATE_MARKER_SIZE: TextUnit = 12.sp
