package com.solthariel.pawchan.media.video

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner

@Composable
internal fun BindVideoPlaybackLifecycle(
    session: VideoPlaybackSession,
    adopted: Boolean = false,
    /** Сессия из [VideoSessionPool]: true/false — играть ли дальше; null — сессия новая. */
    pooled: Boolean? = null,
    autoPlay: Boolean = true,
    retainOnDispose: Boolean = true,
    initialPosition: Long = 0L,
) {
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    val context = LocalContext.current
    val access = LocalVideoPrivacyAccess.current
    val picker = com.solthariel.pawchan.media.LocalMediaPickerPlayback.current
    BindVideoPickerReturn(session, access)
    DisposableEffect(session, lifecycle, access, picker) {
        session.audio(context)
        session.privacy.bind(access)
        val observer =
            LifecycleEventObserver { _, event ->
                when (event) {
                    Lifecycle.Event.ON_STOP -> {
                        picker?.stopped()
                        if (!session.inPip) session.pause()
                    }

                    Lifecycle.Event.ON_RESUME -> {
                        picker?.returned()
                    }

                    else -> {
                        Unit
                    }
                }
            }
        lifecycle.addObserver(observer)
        if (adopted) {
            session.redrawPaused()
        } else if (pooled != null) {
            session.reattach(pooled && autoPlay && access?.isAllowed() != false)
        } else {
            // После закрытого окна PiP тот же файл продолжается с позиции, где его закрыли.
            val resume = VideoPipHandoff.takeResume(session.url)
            resume?.let { session.setSpeed(it.speed) }
            session.startAt(
                resume?.position ?: initialPosition,
                (resume?.playing ?: autoPlay) && access?.isAllowed() != false,
            )
        }
        onDispose {
            lifecycle.removeObserver(observer)
            // Сессию, переданную окну PiP, закрывает уже оно; ушли свайпом — сессия ждёт возврата в запасе.
            if (!session.inPip) {
                if (retainOnDispose) VideoSessionPool.retain(session) else session.close()
            }
        }
    }
}
