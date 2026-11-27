package com.solthariel.pawchan.media.video

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.os.Handler
import androidx.core.content.ContextCompat

internal class VideoPlaybackAudio(
    private val context: Context,
    private val session: VideoPlaybackSession,
) : SessionAudioFocus {
    private val manager = context.getSystemService(AudioManager::class.java)
    private var closed = false
    private var ducked = false
    private val request =
        AudioFocusRequest
            .Builder(AudioManager.AUDIOFOCUS_GAIN)
            .setAudioAttributes(
                AudioAttributes
                    .Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_MOVIE)
                    .build(),
            ).setOnAudioFocusChangeListener(::onFocusChange, Handler(context.mainLooper))
            .build()
    private val lease =
        PlaybackFocusGate(
            { manager.requestAudioFocus(request) == AudioManager.AUDIOFOCUS_REQUEST_GRANTED },
            { manager.abandonAudioFocusRequest(request) },
        )
    private val noisy =
        object : BroadcastReceiver() {
            override fun onReceive(
                context: Context?,
                intent: Intent?,
            ) {
                if (intent?.action == AudioManager.ACTION_AUDIO_BECOMING_NOISY && session.needsAudioFocus) {
                    session.pause()
                }
            }
        }

    init {
        ContextCompat.registerReceiver(
            context,
            noisy,
            IntentFilter(AudioManager.ACTION_AUDIO_BECOMING_NOISY),
            ContextCompat.RECEIVER_NOT_EXPORTED,
        )
    }

    private fun onFocusChange(change: Int) {
        if (closed || !lease.isHeld) return
        when (change) {
            AudioManager.AUDIOFOCUS_LOSS, AudioManager.AUDIOFOCUS_LOSS_TRANSIENT -> {
                val wasAudible = session.needsAudioFocus
                releaseFocus()
                if (wasAudible) session.pause()
            }

            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK -> {
                if (session.needsAudioFocus) {
                    ducked = true
                    session.engine.volume(VIDEO_DUCK_VOLUME)
                }
            }

            AudioManager.AUDIOFOCUS_GAIN -> {
                restoreVolume()
            }
        }
    }

    override fun sync() {
        if (closed) return
        if (session.needsAudioFocus) {
            if (!lease.update(true)) session.pause()
        } else {
            releaseFocus()
        }
    }

    override fun allowUnmute(): Boolean = !closed && lease.update(true)

    private fun restoreVolume() {
        if (ducked) {
            ducked = false
            session.engine.volume(VIDEO_NORMAL_VOLUME)
        }
    }

    private fun releaseFocus() {
        lease.update(false)
        restoreVolume()
    }

    override fun close() {
        if (closed) return
        closed = true
        context.unregisterReceiver(noisy)
        releaseFocus()
    }
}

private const val VIDEO_DUCK_VOLUME = 20
private const val VIDEO_NORMAL_VOLUME = 100

/** One focus request follows the session through viewer → PiP → viewer. */
internal fun VideoPlaybackSession.audio(context: Context): SessionAudioFocus =
    audio ?: VideoPlaybackAudio(context.applicationContext, this).also {
        audio = it
        it.sync()
    }
