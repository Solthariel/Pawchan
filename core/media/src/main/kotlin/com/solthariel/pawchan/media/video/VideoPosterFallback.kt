package com.solthariel.pawchan.media.video

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.solthariel.pawchan.core.media.R

/** Used while loading, on missing URLs and after both thumbnail loaders fail. */
@Composable
internal fun VideoPosterFallback(frame: Bitmap? = LocalVideoPosterStore.current?.frame) {
    if (frame != null) {
        Image(
            bitmap = remember(frame) { frame.asImageBitmap() },
            contentDescription = stringResource(R.string.video_loading),
            contentScale = ContentScale.Fit,
            modifier = Modifier.fillMaxSize(),
        )
    } else {
        Box(
            Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surfaceContainerHigh),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                painterResource(R.drawable.thumbnail_pending),
                contentDescription = stringResource(R.string.video_loading),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(POSTER_PLACEHOLDER_SIZE.dp),
            )
        }
    }
}

private const val POSTER_PLACEHOLDER_SIZE = 48
