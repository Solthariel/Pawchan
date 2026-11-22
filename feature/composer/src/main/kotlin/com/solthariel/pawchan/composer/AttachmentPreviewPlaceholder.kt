package com.solthariel.pawchan.composer

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.solthariel.pawchan.core.ui.PageLoadingIndicator
import com.solthariel.pawchan.feature.composer.R

@Composable
internal fun AttachmentPreviewPlaceholder(failed: Boolean) {
    if (failed) {
        Text(
            stringResource(R.string.attachment_preview_unavailable),
            color = Color.White,
            modifier = Modifier.background(Color.Black.copy(alpha = 0.75f)).padding(4.dp),
        )
    } else {
        PageLoadingIndicator(
            description = "Загрузка предпросмотра вложения",
            size = 24.dp,
            backgroundColor = Color.Black.copy(alpha = 0.75f),
            foregroundColor = Color.White,
        )
    }
}
