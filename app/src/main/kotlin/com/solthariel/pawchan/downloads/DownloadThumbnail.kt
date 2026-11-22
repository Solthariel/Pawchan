package com.solthariel.pawchan.downloads

import android.graphics.Bitmap
import android.util.LruCache
import android.util.Size
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import com.solthariel.pawchan.core.ui.UiIcon
import com.solthariel.pawchan.core.ui.UiIconKind
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException

/** Thumbnail bytes come only from the saved document, never from the network. */
@Composable
internal fun DownloadThumbnail(
    record: DownloadRecord,
    cache: LruCache<String, Bitmap>,
    onOpen: () -> Unit,
) {
    val context = LocalContext.current
    val size = with(LocalDensity.current) { 56.dp.roundToPx().coerceAtLeast(1) }
    val available = record.status == DownloadStatus.SUCCEEDED
    val cacheKey = "${record.id}:$size"
    val bitmap by produceState<Bitmap?>(if (available) cache.get(cacheKey) else null, cacheKey, available) {
        if (!available) {
            value = null
        } else {
            value =
                withContext(Dispatchers.IO) {
                    cache.get(cacheKey) ?: try {
                        context.contentResolver
                            .loadThumbnail(record.destination.toUri(), Size(size, size), null)
                            .also { cache.put(cacheKey, it) }
                    } catch (_: IOException) {
                        null
                    } catch (_: SecurityException) {
                        null
                    } catch (_: IllegalArgumentException) {
                        null
                    }
                }
        }
    }
    val video = record.name.substringAfterLast('.', "").lowercase() in VIDEO_EXTENSIONS
    Box(
        Modifier
            .size(56.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .clickable(enabled = available, onClick = onOpen),
        contentAlignment = Alignment.Center,
    ) {
        val image = bitmap
        if (image == null) {
            Icon(
                painterResource(com.solthariel.pawchan.core.media.R.drawable.thumbnail_unavailable),
                contentDescription = "Предпросмотр недоступен",
                modifier = Modifier.size(24.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.45f),
            )
        } else {
            Image(
                image.asImageBitmap(),
                contentDescription = "Открыть файл",
                modifier = Modifier.matchParentSize(),
                contentScale = ContentScale.Crop,
            )
        }
        if (video && image != null) UiIcon(UiIconKind.PLAY, Color.White, Modifier.size(24.dp))
    }
}

private val VIDEO_EXTENSIONS = setOf("mp4", "webm", "mov", "mkv", "m4v")
