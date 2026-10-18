package com.solthariel.pawchan.media.video

import androidx.compose.runtime.staticCompositionLocalOf

/** A protected host can prohibit moving its media into an unprotected external window. */
val LocalVideoPipAllowed = staticCompositionLocalOf { true }
