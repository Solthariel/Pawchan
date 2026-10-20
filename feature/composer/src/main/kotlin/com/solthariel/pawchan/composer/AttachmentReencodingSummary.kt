package com.solthariel.pawchan.composer

import android.graphics.BitmapFactory
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.solthariel.pawchan.core.ui.formatByteSizeForUi
import com.solthariel.pawchan.feature.composer.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Header/EXIF and debounced output-size calculation stay off Main. */
@Composable
internal fun AttachmentReencodingSummary(
    file: ComposerAttachment,
    settings: ImageReencoding,
) {
    val source by produceState<ReencodingSource?>(null, file.id, file.source.content) {
        value = withContext(Dispatchers.IO) { readReencodingSource(file.source.content) }
    }
    val pending = remember(file, settings) { file.copy(options = file.options.copy(reencoding = settings)) }
    val name = if (pending.validName) pending.displayName else stringResource(R.string.attachment_result_invalid_name)
    val dimensions = source?.let { reencodingDimensions(it.width, it.height, settings)?.oriented(it.orientation) }
    val resolution =
        dimensions?.let { "${it.width}×${it.height}" }
            ?: stringResource(R.string.attachment_result_resolution_unknown)
    val result by rememberReencodingSize(pending)
    val size = if (result.calculating) "…" else result.bytes?.let(::formatByteSizeForUi) ?: "?"
    Row(
        Modifier.fillMaxWidth().semantics(mergeDescendants = true) {},
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(
            name,
            modifier = Modifier.weight(1f, fill = false),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.MiddleEllipsis,
        )
        Text(
            "· $resolution · $size",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
        )
    }
}

internal data class ReencodingSource(
    val width: Int,
    val height: Int,
    val orientation: Int,
)

internal fun readReencodingSource(content: ByteArray): ReencodingSource? {
    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    BitmapFactory.decodeByteArray(content, 0, content.size, bounds)
    return if (bounds.outWidth > 0 && bounds.outHeight > 0) {
        ReencodingSource(bounds.outWidth, bounds.outHeight, imageOrientation(content))
    } else {
        null
    }
}
