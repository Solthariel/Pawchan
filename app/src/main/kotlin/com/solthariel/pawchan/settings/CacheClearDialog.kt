package com.solthariel.pawchan.settings

import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalResources
import com.solthariel.pawchan.core.ui.TransientNotice
import com.solthariel.pawchan.R.string as L
import com.solthariel.pawchan.core.ui.AppAlertDialog as AlertDialog

@Composable
internal fun CacheClearDialog(
    close: () -> Unit,
    clear: () -> Unit,
) {
    val res = LocalResources.current

    AlertDialog(
        onDismissRequest = close,
        title = { Text(res.getString(L.ui_clear_image_cache)) },
        text = {
            Text(
                res.getString(L.ui_thumbnails_and_image_cache_will) +
                    res.getString(L.ui_open_media_saved_threads_drafts),
            )
        },
        confirmButton = { TextButton(onClick = clear) { Text(res.getString(L.ui_clear)) } },
        dismissButton = { TextButton(onClick = close) { Text(res.getString(L.ui_cancel)) } },
    )
}

@Composable
internal fun CacheNotice(
    text: String,
    close: () -> Unit,
) {
    TransientNotice(text, onDismiss = close)
}
