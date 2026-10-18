package com.solthariel.pawchan.media.video

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import androidx.core.content.ContextCompat

internal class VideoPlaybackAudio(
    private val context: Context,
    private val session: VideoPlaybackSession,
) : AutoCloseable {
    private val manager = context.getSystemService(AudioManager::class.java)
    private var focused = false
    private val request =
        AudioFocusRequest
            .Builder(AudioManager.AUDIOFOCUS_GAIN)
            .setAudioAttributes(
                AudioAttributes
                    .Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_MOVIE)
                    .build(),
            ).setOnAudioFocusChangeListener { change ->
                when (change) {
                    AudioManager.AUDIOFOCUS_LOSS, AudioManager.AUDIOFOCUS_LOSS_TRANSIENT -> session.pause()
                    AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK -> session.engine.volume(VIDEO_DUCK_VOLUME)
                    AudioManager.AUDIOFOCUS_GAIN -> session.engine.volume(VIDEO_NORMAL_VOLUME)
                }
            }.build()
    private val noisy =
        object : BroadcastReceiver() {
            override fun onReceive(
                context: Context?,
                intent: Intent?,
            ) {
                if (intent?.action == AudioManager.ACTION_AUDIO_BECOMING_NOISY) session.pause()
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

    fun update(playing: Boolean) {
        if (playing && !focused) {
            focused = manager.requestAudioFocus(request) == AudioManager.AUDIOFOCUS_REQUEST_GRANTED
            if (!focused) session.pause()
        } else if (!playing && focused) {
            manager.abandonAudioFocusRequest(request)
            focused = false
        }
    }

    override fun close() {
        context.unregisterReceiver(noisy)
        manager.abandonAudioFocusRequest(request)
    }
}

private const val VIDEO_DUCK_VOLUME = 20
private const val VIDEO_NORMAL_VOLUME = 100
