package com.solthariel.pawchan.media.video

/** Main-thread ownership of one system audio focus request, shared across viewer and PiP. */
internal class PlaybackFocusGate(
    private val acquire: () -> Boolean,
    private val abandon: () -> Unit,
) {
    var isHeld: Boolean = false
        private set

    fun update(needed: Boolean): Boolean {
        if (!needed) {
            if (isHeld) {
                isHeld = false
                abandon()
            }
            return true
        }
        if (!isHeld) isHeld = acquire()
        return isHeld
    }
}

internal fun VideoSoundState.needsFocus(playing: Boolean): Boolean = playing && hasAudio && !muted

internal val VideoPlaybackSession.needsAudioFocus: Boolean
    get() = sound.value.needsFocus(wantsPlayback && status.value.playing)

internal interface SessionAudioFocus : AutoCloseable {
    fun sync()

    fun allowUnmute(): Boolean
}
