package com.solthariel.pawchan.media

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import coil3.ImageLoader
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun BindAttachmentImagePreload(
    attachments: List<MediaAttachmentUi>,
    imageLoader: ImageLoader,
) {
    val context = LocalContext.current.applicationContext
    val preloader = remember(context, imageLoader) { AttachmentImagePreloader(context, imageLoader) }
    val hideStickers = com.solthariel.pawchan.core.ui.LocalPostDisplayPreferences.current.hideStickers
    LaunchedEffect(preloader, attachments, hideStickers) {
        val visible =
            withContext(Dispatchers.Default) {
                if (hideStickers) attachments.filterNot { it.isSticker } else attachments
            }
        preloader.preload(visible)
    }
}
