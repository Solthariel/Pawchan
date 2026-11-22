package com.solthariel.pawchan.media.video

/** Main-thread gate: mounting a window is not the same as presenting media from the current file. */
internal class MpvFirstVideoFrame<T : Any> {
    var surface: T? = null
        private set
    var playbackReady = false
        private set
    private var announced = false

    fun resetPlayback() {
        playbackReady = false
        mount(null)
    }

    fun mount(next: T?) {
        if (surface === next) return
        surface = next
        announced = false
    }

    /** Later seeks also emit PLAYBACK_RESTART; never re-arm output for the same surface. */
    fun playbackRestarted(): Boolean {
        if (playbackReady) return false
        playbackReady = true
        return true
    }

    fun onTextureUpdate(source: T): Boolean {
        if (surface !== source || !playbackReady || announced) return false
        announced = true
        return true
    }
}
