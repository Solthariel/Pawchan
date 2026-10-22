package com.solthariel.pawchan.media.video

import androidx.compose.runtime.staticCompositionLocalOf
import kotlinx.coroutines.flow.Flow

/** The app owns the lock and its timeout; media can observe but cannot unlock it. */
interface VideoPrivacyAccess {
    val changes: Flow<Boolean>

    fun isAllowed(): Boolean
}

/** An absent host policy means that the media is not PIN-protected. */
val LocalVideoPrivacyAccess = staticCompositionLocalOf<VideoPrivacyAccess?> { null }

/** Additional host restriction; never replaces the live permission check. */
val LocalVideoPipAllowed = staticCompositionLocalOf { true }
