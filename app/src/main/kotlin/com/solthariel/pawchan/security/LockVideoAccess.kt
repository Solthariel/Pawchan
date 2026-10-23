package com.solthariel.pawchan.security

import com.solthariel.pawchan.media.video.VideoPrivacyAccess

/** Media reads the current privacy decision synchronously; notifications are observation only. */
internal class LockVideoAccess(
    private val session: AppLockSession,
) : VideoPrivacyAccess {
    override val changes = session.accessChanges

    override fun isAllowed(): Boolean = session.isAccessAllowed()
}
