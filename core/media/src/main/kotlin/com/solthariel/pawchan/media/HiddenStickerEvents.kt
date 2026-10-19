package com.solthariel.pawchan.media

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.staticCompositionLocalOf
import com.solthariel.pawchan.core.ui.LocalPostDisplayPreferences

fun interface HiddenStickerEvents {
    suspend fun hidden(identity: String)
}

val LocalHiddenStickerEvents = staticCompositionLocalOf { HiddenStickerEvents {} }

/** Applies to posts, previews and catalog; source lists stay intact for export. */
@Composable
fun visiblePostAttachments(display: PostMediaDisplay): List<MediaAttachmentUi> {
    val hide = LocalPostDisplayPreferences.current.hideStickers
    val events = LocalHiddenStickerEvents.current
    val hidden = if (hide) display.stickers else emptyList()
    LaunchedEffect(hidden, events) {
        hidden.forEach { attachment ->
            events.hidden(
                com.solthariel.pawchan.domain.statistics
                    .usageMediaIdentity(attachment.originalUrl),
            )
        }
    }
    return display.visible(hide)
}
