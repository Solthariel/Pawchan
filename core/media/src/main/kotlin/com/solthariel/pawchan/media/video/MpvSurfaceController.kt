package com.solthariel.pawchan.media.video

import android.graphics.SurfaceTexture
import java.util.concurrent.ConcurrentHashMap

/** Engine-owned access to its serialized worker and main-thread callbacks. */
internal interface MpvSurfaceHost {
    val token: Int
    val closed: Boolean
    val handle: Long

    fun onMain(action: () -> Unit)

    fun onWorker(
        token: Int,
        action: () -> Unit,
    )

    fun onEvent(event: VideoEvent)

    fun setProperty(
        name: String,
        value: String,
    )
}

/** Owns texture slots and coordinates their lifetime with the native renderer. */
internal class MpvSurfaceController(
    private val host: MpvSurfaceHost,
) {
    @Volatile private var current: MpvSurfaceSlot? = null
    private val slots = ConcurrentHashMap<SurfaceTexture, MpvSurfaceSlot>()
    private val firstFrame = MpvFirstVideoFrame<MpvSurfaceSlot>()
    private val native = MpvSurfaceWorker(host, ::mounted)
    val output = MpvVideoOutput(::created, ::resized, ::destroyed, ::frame)

    fun prepareStop() = firstFrame.resetPlayback()

    fun playbackRestarted() {
        if (firstFrame.playbackRestarted()) firstFrame.surface?.let(::redrawPaused)
    }

    /** Called on the worker after initialization. */
    fun mountCurrent(handle: Long) {
        current?.takeUnless { it.retired }?.let { native.mount(handle, it) }
    }

    /** Called on the worker after the mpv instance has been destroyed. */
    fun engineDestroyed() = native.engineDestroyed()

    private fun created(
        texture: SurfaceTexture,
        width: Int,
        height: Int,
    ) {
        if (host.closed) {
            texture.release()
            return
        }
        val slot = MpvSurfaceSlot(texture, width, height)
        slots[texture] = slot
        current = slot
        firstFrame.mount(null)
        host.onEvent(VideoEvent.VIDEO_OUTPUT_PENDING)
        val token = host.token
        host.onWorker(token) { native.created(slot) }
    }

    private fun resized(
        texture: SurfaceTexture,
        width: Int,
        height: Int,
    ) {
        val slot = slots[texture] ?: return
        slot.width = width
        slot.height = height
        if (current === slot) host.setProperty("android-surface-size", "${width}x$height")
    }

    private fun destroyed(texture: SurfaceTexture) {
        val slot = slots.remove(texture) ?: return
        slot.retired = true
        if (current === slot) {
            current = null
            firstFrame.mount(null)
        }
        host.onWorker(host.token) { native.destroyed(slot) }
    }

    private fun frame(texture: SurfaceTexture) {
        val slot = current ?: return
        if (host.closed || slot.texture !== texture || !firstFrame.onTextureUpdate(slot)) return
        val token = host.token
        // Let TextureView finish the update before a listener can synchronously request a poster.
        host.onMain {
            if (!host.closed && host.token == token && firstFrame.surface === slot) {
                host.onEvent(VideoEvent.VIDEO_OUTPUT)
            }
        }
    }

    private fun mounted(
        slot: MpvSurfaceSlot,
        token: Int,
    ) {
        host.onMain {
            if (token == host.token && !host.closed && current === slot) {
                firstFrame.mount(slot)
                if (firstFrame.playbackReady) redrawPaused(slot)
            }
        }
    }

    private fun redrawPaused(slot: MpvSurfaceSlot) {
        val token = host.token
        host.onWorker(token) {
            if (token == host.token && !host.closed && current === slot) native.redrawPaused(slot)
        }
    }
}
