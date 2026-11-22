package com.solthariel.pawchan.media.video

import android.graphics.Bitmap
import android.graphics.SurfaceTexture
import android.view.Surface
import android.view.TextureView
import android.widget.FrameLayout

/** Android surface ownership stays with the engine, including handoff to and from PiP. */
internal class MpvVideoOutput(
    private val created: (SurfaceTexture, Int, Int) -> Unit,
    private val resized: (SurfaceTexture, Int, Int) -> Unit,
    private val destroyed: (SurfaceTexture) -> Unit,
    private val frame: (SurfaceTexture) -> Unit,
) {
    private var host: FrameLayout? = null
    private var texture: TextureView? = null

    fun attach(next: FrameLayout) {
        if (host === next) return
        host?.removeView(texture)
        host = next
        val view = TextureView(next.context)
        texture = view
        view.surfaceTextureListener =
            object : TextureView.SurfaceTextureListener {
                override fun onSurfaceTextureAvailable(
                    surface: SurfaceTexture,
                    width: Int,
                    height: Int,
                ) = created(surface, width, height)

                override fun onSurfaceTextureSizeChanged(
                    surface: SurfaceTexture,
                    width: Int,
                    height: Int,
                ) = resized(surface, width, height)

                override fun onSurfaceTextureDestroyed(surface: SurfaceTexture): Boolean {
                    destroyed(surface)
                    return false
                }

                override fun onSurfaceTextureUpdated(surface: SurfaceTexture) = frame(surface)
            }
        next.addView(view, FrameLayout.LayoutParams(-1, -1))
    }

    fun detach(view: FrameLayout) {
        if (host !== view) return
        host?.removeView(texture)
        texture = null
        host = null
    }

    fun close() {
        texture?.surfaceTexture?.let(destroyed)
        host?.removeView(texture)
        texture = null
        host = null
    }

    fun snapshot(): Bitmap? {
        val view = texture?.takeIf { it.isAvailable && it.width > 0 && it.height > 0 } ?: return null
        val factor = minOf(1f, VIDEO_POSTER_MAX_EDGE.toFloat() / maxOf(view.width, view.height))
        return view.getBitmap(maxOf(1, (view.width * factor).toInt()), maxOf(1, (view.height * factor).toInt()))
    }
}

internal class MpvSurfaceSlot(
    val texture: SurfaceTexture,
    width: Int,
    height: Int,
) {
    @Volatile var width: Int = width

    @Volatile var height: Int = height
    val surface = Surface(texture)
    val nativeReferences = mutableListOf<Long>()

    @Volatile var retired = false

    fun release() {
        surface.release()
        texture.release()
    }
}
