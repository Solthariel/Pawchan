package com.solthariel.pawchan.media

/** A scoped handoff for the external folder picker; ordinary backgrounding never requests playback. */
class MediaPickerPlayback {
    private var playing: (() -> Boolean)? = null
    private var resume: (() -> Unit)? = null
    private var resumed = false
    private var waiting = false
    private var restore = false

    internal fun bind(
        isPlaying: () -> Boolean,
        onResume: () -> Unit,
        foreground: Boolean,
    ) {
        clear()
        playing = isPlaying
        resume = onResume
        resumed = foreground
    }

    internal fun clear() {
        playing = null
        resume = null
        waiting = false
        restore = false
        resumed = false
    }

    internal fun started() {
        if (waiting) return
        waiting = true
        restore = playing?.invoke() == true
    }

    internal fun stopped() {
        resumed = false
    }

    internal fun returned() {
        resumed = true
        restoreIfReady()
    }

    internal fun completed() {
        waiting = false
        restoreIfReady()
    }

    private fun restoreIfReady() {
        if (!waiting && resumed && restore) {
            restore = false
            resume?.invoke()
        }
    }
}
