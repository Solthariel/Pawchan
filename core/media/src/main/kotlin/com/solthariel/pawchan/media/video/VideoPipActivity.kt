package com.solthariel.pawchan.media.video

import android.app.PendingIntent
import android.app.PictureInPictureParams
import android.app.RemoteAction
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.res.Configuration
import android.graphics.Color
import android.graphics.Rect
import android.graphics.drawable.Icon
import android.os.Bundle
import android.util.Log
import android.widget.FrameLayout
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.core.content.ContextCompat
import androidx.core.view.doOnLayout
import androidx.lifecycle.Lifecycle
import com.solthariel.pawchan.core.media.BuildConfig
import com.solthariel.pawchan.core.media.R
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

/**
 * Окно «картинка в картинке»: отдельная задача, но плеер тот же —
 * просмотрщик передаёт играющую сессию, окно лишь переносит её поверхность (без переоткрытия и
 * перемотки), кнопки −10 с / ▶ / +10 с. Разворот возвращает сессию просмотрщику, закрытие
 * останавливает видео.
 */
class VideoPipActivity : ComponentActivity() {
    private val scope = MainScope()
    private var session: VideoPlaybackSession? = null
    private var audio: VideoPlaybackAudio? = null
    private var request: VideoPipRequest? = null
    private var container: FrameLayout? = null
    private var entered = false
    private val privacy =
        VideoPipPrivacy(
            allowed = { request?.access?.isAllowed() != false },
            revoke = {
                session?.pause()
                container?.let { view ->
                    session?.engine?.detach(view)
                    view.removeAllViews()
                }
                rememberPosition()
                session?.close()
                session = null
                finishAndRemoveTask()
            },
        )
    private var receiving = false
    private val controls =
        object : BroadcastReceiver() {
            override fun onReceive(
                context: Context?,
                intent: Intent?,
            ) {
                if (intent?.action == Intent.ACTION_SCREEN_OFF || !privacy.allowed()) {
                    privacy.revoke()
                    return
                }
                val player = session ?: return
                when (intent?.getIntExtra(EXTRA_CONTROL, 0)) {
                    CONTROL_BACK -> player.seekTo(player.status.value.position - SKIP_MS)
                    CONTROL_PLAY -> player.togglePlay()
                    CONTROL_FORWARD -> player.seekTo(player.status.value.position + SKIP_MS)
                }
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val request = VideoPipHandoff.pending
        VideoPipHandoff.pending = null
        if (request == null || request.access?.isAllowed() == false) {
            request?.session?.pause()
            request?.session?.inPip = false
            finish()
            return
        }
        val view = FrameLayout(this).apply { setBackgroundColor(Color.BLACK) }
        setContentView(view)
        container = view
        val player = request.session
        session = player
        this.request = request
        audio = player.audio(this)
        ContextCompat.registerReceiver(
            this,
            controls,
            IntentFilter(ACTION_CONTROL).apply { addAction(Intent.ACTION_SCREEN_OFF) },
            ContextCompat.RECEIVER_NOT_EXPORTED,
        )
        receiving = true
        updatePipParams(player.status.value)
        scope.launch {
            request.access?.changes?.collect { allowed -> if (!allowed) privacy.revoke() }
        }
        scope.launch {
            player.status
                .map { it.playing }
                .distinctUntilChanged()
                .collect { audio?.update(it) }
        }
        scope.launch {
            player.status.map { it.playing to it.videoAspect }.distinctUntilChanged().collect {
                updatePipParams(player.status.value)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        if (!entered && session != null) {
            entered = true
            // Размер контейнера нужен для sourceRectHint: окно сжимается ровно из кадра видео.
            container?.doOnLayout { enterPip() }
        }
    }

    private fun enterPip() {
        if (!privacy.allowed()) {
            privacy.revoke()
            return
        }
        val status = session?.status?.value
        val accepted = if (status == null) false else tryEnterPip(status)
        if (BuildConfig.DEBUG) Log.i("PawMpv", "pip accepted=$accepted")
        if (commitPipTransfer(accepted) {
                // Keep the viewer surface until Android has actually accepted PiP.
                val player = session
                val view = container
                if (player != null && view != null) {
                    player.engine.attach(view)
                    player.redrawPaused()
                }
                request?.onShown?.invoke()
            }
        ) {
            return
        } else {
            Toast.makeText(this, R.string.video_pip_failed, Toast.LENGTH_SHORT).show()
            // Просмотрщик ещё открыт: сессия возвращается ему как есть.
            session?.inPip = false
            session = null
            finish()
        }
    }

    private fun tryEnterPip(status: VideoPlaybackStatus): Boolean =
        try {
            enterPictureInPictureMode(pipParams(status))
        } catch (_: IllegalArgumentException) {
            // Like mpv-android, retry square if the device rejects the video aspect.
            try {
                enterPictureInPictureMode(pipParams(status, forceSquare = true))
            } catch (_: IllegalStateException) {
                false
            } catch (_: IllegalArgumentException) {
                false
            }
        } catch (_: IllegalStateException) {
            false
        }

    override fun onPictureInPictureModeChanged(
        isInPictureInPictureMode: Boolean,
        newConfig: Configuration,
    ) {
        super.onPictureInPictureModeChanged(isInPictureInPictureMode, newConfig)
        if (isInPictureInPictureMode || isFinishing) return
        // Развернули — окно ещё видно: просмотрщик откроется на этом же видео. Закрыли крестиком —
        // активность уже остановлена: видео просто останавливается.
        if (lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)) {
            returnToApp()
        } else {
            rememberPosition()
            finish()
        }
    }

    override fun onStop() {
        super.onStop()
        // Окно смахнули: видео останавливается вместе с ним.
        if (!isChangingConfigurations && !isFinishing) {
            rememberPosition()
            finish()
        }
    }

    private fun returnToApp() {
        if (BuildConfig.DEBUG) Log.i("PawMpv", "pip returning to viewer")
        if (!privacy.allowed()) {
            privacy.revoke()
            return
        }
        val player = session
        val request = request
        if (player != null && request != null) {
            VideoPipHandoff.park(player)
            session = null
            VideoPipHandoff.returning.value = VideoPipReturn(request.attachment)
        }
        packageManager.getLaunchIntentForPackage(packageName)?.let { launch ->
            startActivity(launch.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        }
        finish()
    }

    private fun rememberPosition() {
        val player = session ?: return
        val resume = VideoPipResume(player.status.value.position, false, player.sound.value.speed)
        VideoPipHandoff.remember(player.url, resume)
    }

    override fun onDestroy() {
        if (BuildConfig.DEBUG) Log.i("PawMpv", "pip destroyed, ownsSession=${session != null}")
        if (receiving) unregisterReceiver(controls)
        session?.close()
        session = null
        scope.cancel()
        super.onDestroy()
    }

    private fun updatePipParams(status: VideoPlaybackStatus) {
        if (isFinishing || !privacy.allowed()) return
        try {
            setPictureInPictureParams(pipParams(status))
        } catch (_: IllegalArgumentException) {
            // Some Android versions impose narrower aspect limits than documented.
            try {
                setPictureInPictureParams(pipParams(status, forceSquare = true))
            } catch (_: IllegalStateException) {
                // Activity is already leaving.
            } catch (_: IllegalArgumentException) {
                // The system rejected even square PiP parameters.
            }
        } catch (_: IllegalStateException) {
            // Activity is already leaving; no further parameter updates are useful.
        }
    }

    private fun pipParams(
        status: VideoPlaybackStatus,
        forceSquare: Boolean = false,
    ): PictureInPictureParams {
        val aspect = if (forceSquare) android.util.Rational(1, 1) else request?.dimensions.pipAspect(status.videoAspect)
        return PictureInPictureParams
            .Builder()
            .setActions(pipActions(status.playing))
            .setAspectRatio(aspect)
            .apply { container?.videoBounds(aspect.toFloat())?.let(::setSourceRectHint) }
            .build()
    }
}

private fun VideoPipActivity.pipActions(playing: Boolean): List<RemoteAction> =
    listOf(
        pipAction(CONTROL_BACK, android.R.drawable.ic_media_rew, getString(R.string.video_pip_back)),
        if (playing) {
            pipAction(CONTROL_PLAY, android.R.drawable.ic_media_pause, getString(R.string.video_pause))
        } else {
            pipAction(CONTROL_PLAY, android.R.drawable.ic_media_play, getString(R.string.video_play))
        },
        pipAction(CONTROL_FORWARD, android.R.drawable.ic_media_ff, getString(R.string.video_pip_forward)),
    )

private fun VideoPipActivity.pipAction(
    control: Int,
    icon: Int,
    title: String,
): RemoteAction {
    val intent = Intent(ACTION_CONTROL).setPackage(packageName).putExtra(EXTRA_CONTROL, control)
    val pending =
        PendingIntent.getBroadcast(
            this,
            control,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
    return RemoteAction(Icon.createWithResource(this, icon), title, title, pending)
}

/** Кадр видео внутри полноэкранного контейнера (вписан по пропорциям), в координатах окна. */
private fun FrameLayout.videoBounds(aspect: Float): Rect? {
    if (width <= 0 || height <= 0) return null
    val frameWidth = minOf(width.toFloat(), height * aspect)
    val frameHeight = frameWidth / aspect
    val left = ((width - frameWidth) / 2).toInt()
    val top = ((height - frameHeight) / 2).toInt()
    val origin = IntArray(2).also(::getLocationInWindow)
    return Rect(left, top, left + frameWidth.toInt(), top + frameHeight.toInt()).apply { offset(origin[0], origin[1]) }
}

private const val ACTION_CONTROL = "com.solthariel.pawchan.media.video.PIP_CONTROL"
private const val EXTRA_CONTROL = "control"
private const val CONTROL_BACK = 1
private const val CONTROL_PLAY = 2
private const val CONTROL_FORWARD = 3
private const val SKIP_MS = 10_000L
