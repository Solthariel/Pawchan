package com.solthariel.pawchan.media.video

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
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
) {
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    val context = LocalContext.current
    val audio = remember(session) { VideoPlaybackAudio(context, session) }
    LaunchedEffect(session, audio) { session.status.collect { audio.update(it.playing) } }
    DisposableEffect(session, lifecycle) {
        val observer =
            LifecycleEventObserver { _, event ->
                if (event == Lifecycle.Event.ON_STOP && !session.inPip) session.pause()
            }
        lifecycle.addObserver(observer)
        if (adopted) {
            session.redrawPaused()
        } else if (pooled != null) {
            session.reattach(pooled && autoPlay)
        } else {
            // После закрытого окна PiP тот же файл продолжается с позиции, где его закрыли.
            val resume = VideoPipHandoff.takeResume(session.url)
            resume?.let { session.setSpeed(it.speed) }
            session.startAt(resume?.position ?: 0L, resume?.playing ?: autoPlay)
        }
        onDispose {
            lifecycle.removeObserver(observer)
            audio.close()
            // Сессию, переданную окну PiP, закрывает уже оно; ушли свайпом — сессия ждёт возврата в запасе.
            if (!session.inPip) VideoSessionPool.retain(session)
        }
    }
}
