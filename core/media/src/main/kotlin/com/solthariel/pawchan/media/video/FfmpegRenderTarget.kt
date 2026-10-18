package com.solthariel.pawchan.media.video

import android.view.Gravity
import android.widget.FrameLayout
import androidx.core.view.isNotEmpty
import com.solthariel.pawchan.ffmpeg.VideoPlayer

/**
 * Surface identity belongs to the current AndroidView, not a previous gallery item. The video view is
 * created with the container's own context, so a playing session can move between the viewer and PiP.
 */
internal class FfmpegRenderTarget {
    private var container: FrameLayout? = null

    fun mount(
        player: VideoPlayer,
        initialized: Boolean,
    ) {
        val target = container ?: return
        if (!initialized || target.isNotEmpty()) return
        VideoHandoffDiagnostics.record(
            player,
            "target_mount host=${System.identityHashCode(target)}",
        )
        target.addView(
            player.getVideoView(target.context),
            FrameLayout.LayoutParams(-1, -1, Gravity.CENTER),
        )
    }

    fun attach(
        view: FrameLayout,
        player: VideoPlayer?,
        initialized: Boolean,
    ) {
        if (container === view) return
        clear()
        player?.releaseVideoView()
        container = view
        player?.let { mount(it, initialized) }
    }

    fun detach(
        view: FrameLayout,
        player: VideoPlayer?,
    ) {
        if (container === view) {
            player?.releaseVideoView()
            clear()
            container = null
        }
    }

    fun clear() {
        container?.removeAllViews()
    }
}
