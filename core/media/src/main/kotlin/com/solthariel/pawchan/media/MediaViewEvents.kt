package com.solthariel.pawchan.media

import androidx.compose.runtime.staticCompositionLocalOf

fun interface MediaViewEvents {
    fun opened(attachment: MediaAttachmentUi)
}

val LocalMediaViewEvents = staticCompositionLocalOf { MediaViewEvents {} }
