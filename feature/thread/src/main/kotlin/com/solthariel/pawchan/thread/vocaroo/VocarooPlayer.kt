package com.solthariel.pawchan.thread.vocaroo

import android.media.AudioAttributes
import android.media.MediaPlayer
import android.os.Handler
import android.os.Looper
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import java.io.File
import java.io.IOException

@Immutable
internal data class VocarooState(
    val id: String? = null,
    val preparing: Boolean = false,
    val playing: Boolean = false,
    val failed: Boolean = false,
    /** Доля скачанного файла, пока идёт загрузка; null — размер неизвестен. */
    val progress: Float? = null,
    val position: Int = 0,
    val duration: Int = 0,
)

/**
 * Один плеер на тред: новая запись останавливает прежнюю; звук качается только после ▶ — через
 * [VocarooDownloads] в кэш, затем играется локальный файл (перемотка и повтор мгновенные).
 */
internal class VocarooPlayer(
    private val downloads: VocarooDownloads,
) {
    var state by mutableStateOf(VocarooState())
        private set
    private val durations = mutableStateMapOf<String, Int>()
    private var media: MediaPlayer? = null
    private val scope: CoroutineScope = MainScope()
    private var download: Job? = null
    private var token = 0
    private val handler = Handler(Looper.getMainLooper())
    private val tick =
        object : Runnable {
            override fun run() {
                val player = media ?: return
                if (!state.playing) return
                state = state.copy(position = player.currentPosition)
                handler.postDelayed(this, TICK_MILLIS)
            }
        }

    fun duration(id: String): Int? = durations[id]

    fun toggle(id: String) {
        val player = media
        when {
            state.id != id || player == null || state.failed -> start(id)
            state.preparing -> Unit
            state.playing -> pause()
            else -> resume(player)
        }
    }

    fun seek(
        id: String,
        fraction: Float,
    ) {
        val player = media ?: return
        if (state.id != id || state.preparing || state.duration <= 0) return
        val position = (fraction.coerceIn(0f, 1f) * state.duration).toInt()
        player.seekTo(position)
        state = state.copy(position = position)
    }

    fun pause() {
        if (!state.playing) return
        media?.pause()
        handler.removeCallbacks(tick)
        state = state.copy(playing = false)
    }

    fun release() {
        token++
        download?.cancel()
        download = null
        handler.removeCallbacks(tick)
        media?.release()
        media = null
        state = VocarooState()
    }

    private fun resume(player: MediaPlayer) {
        player.start()
        state = state.copy(playing = true)
        handler.removeCallbacks(tick)
        handler.post(tick)
    }

    private fun start(id: String) {
        release()
        state = VocarooState(id = id, preparing = true)
        val cached = downloads.cached(id)
        if (cached != null) play(id, cached) else fetch(id, token)
    }

    /** Повторы идут внутри [VocarooDownloads.fetch]: кнопка всё это время показывает загрузку. */
    private fun fetch(
        id: String,
        generation: Int,
    ) {
        var percent: Int? = -1
        download =
            scope.launch(Dispatchers.Main.immediate) {
                val file =
                    downloads.fetch(id) { fraction ->
                        val next = fraction?.let { (it * PERCENT).toInt() }
                        if (next != percent) {
                            percent = next
                            handler.post { if (token == generation) state = state.copy(progress = fraction) }
                        }
                    }
                if (token != generation) return@launch
                download = null
                if (file != null) play(id, file) else fail(id)
            }
    }

    private fun play(
        id: String,
        file: File,
    ) {
        val player = MediaPlayer()
        media = player
        player.setAudioAttributes(
            AudioAttributes
                .Builder()
                .setUsage(AudioAttributes.USAGE_MEDIA)
                .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                .build(),
        )
        player.setOnPreparedListener { prepared -> if (media === prepared) onPrepared(id, prepared) }
        player.setOnCompletionListener { done ->
            if (media === done) {
                handler.removeCallbacks(tick)
                done.seekTo(0)
                state = state.copy(playing = false, position = 0)
            }
        }
        player.setOnErrorListener { failed, _, _ ->
            // Битый файл в кэше не должен ломать каждую следующую попытку.
            if (media === failed) {
                file.delete()
                fail(id)
            }
            true
        }
        try {
            player.setDataSource(file.path)
            player.prepareAsync()
        } catch (_: IOException) {
            file.delete()
            fail(id)
        } catch (_: IllegalStateException) {
            fail(id)
        }
    }

    private fun onPrepared(
        id: String,
        player: MediaPlayer,
    ) {
        val duration = player.duration.coerceAtLeast(0)
        durations[id] = duration
        player.start()
        state = state.copy(preparing = false, playing = true, duration = duration, progress = null)
        handler.removeCallbacks(tick)
        handler.post(tick)
    }

    private fun fail(id: String) {
        release()
        state = VocarooState(id = id, failed = true)
    }
}

internal val LocalVocarooPlayer = staticCompositionLocalOf<VocarooPlayer?> { null }

/** Живёт, пока открыт тред; при сворачивании приложения звук ставится на паузу. */
@Composable
internal fun rememberVocarooPlayer(
    transport: VocarooTransport?,
    fallback: OkHttpClient,
): VocarooPlayer? {
    val context = LocalContext.current.applicationContext
    val player =
        remember(context, transport, fallback) {
            val network = transport ?: OkHttpVocarooTransport(fallback)
            VocarooPlayer(VocarooDownloads(network, File(context.cacheDir, CACHE_DIRECTORY)))
        }
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    DisposableEffect(player, lifecycle) {
        val observer = LifecycleEventObserver { _, event -> if (event == Lifecycle.Event.ON_STOP) player.pause() }
        lifecycle.addObserver(observer)
        onDispose {
            lifecycle.removeObserver(observer)
            player.release()
        }
    }
    return player
}

private const val TICK_MILLIS = 250L
private const val PERCENT = 100
private const val CACHE_DIRECTORY = "vocaroo"
