package com.solthariel.pawchan.media

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf

/** Shared by the pages of one open gallery, like the reference LOCKED_USER flag. */
internal class MediaChromeState {
    var visible by mutableStateOf(true)
}

internal val LocalMediaChromeState = staticCompositionLocalOf<MediaChromeState?> { null }
