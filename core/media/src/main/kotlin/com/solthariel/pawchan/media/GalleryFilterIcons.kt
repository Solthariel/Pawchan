package com.solthariel.pawchan.media

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.solthariel.pawchan.core.ui.APP_TOOLBAR_BUTTON_SIZE
import com.solthariel.pawchan.core.ui.UiIcon
import com.solthariel.pawchan.core.ui.UiIconKind

@Composable
internal fun GalleryIconButton(
    description: String,
    action: () -> Unit,
    content: @Composable () -> Unit,
) {
    Box(
        Modifier
            .size(APP_TOOLBAR_BUTTON_SIZE)
            .clip(CircleShape)
            .semantics { contentDescription = description }
            .clickable(onClick = action),
        contentAlignment = Alignment.Center,
    ) {
        content()
    }
}

@Composable
internal fun GalleryFilterIcon(
    filter: GalleryFilter,
    color: Color,
) {
    if (filter == GalleryFilter.GIF) {
        Text("GIF", color = color, fontSize = 10.sp, fontWeight = FontWeight.Bold)
    } else {
        val kind =
            when (filter) {
                GalleryFilter.ALL -> UiIconKind.GRID
                GalleryFilter.IMAGES -> UiIconKind.IMAGE
                else -> UiIconKind.VIDEO
            }
        UiIcon(kind, color, Modifier.size(20.dp))
    }
}

@Composable
internal fun GallerySelectIcon(color: Color) {
    UiIcon(UiIconKind.CHECK_BOX, color, Modifier.size(24.dp))
}
