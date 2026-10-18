package com.solthariel.pawchan.media

import androidx.compose.runtime.staticCompositionLocalOf
import com.solthariel.pawchan.domain.settings.MediaPreferences

/** Immutable settings supplied by the host; media owns no settings repository. */
val LocalMediaPreferences = staticCompositionLocalOf { MediaPreferences() }
internal val LocalViewerAutoPlay = staticCompositionLocalOf { true }
