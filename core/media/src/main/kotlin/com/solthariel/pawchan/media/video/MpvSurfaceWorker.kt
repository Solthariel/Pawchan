package com.solthariel.pawchan.media.video

import io.github.yuroyami.libmpvkt.MpvNode
import io.github.yuroyami.libmpvkt.NodeCodec
import io.github.yuroyami.libmpvkt.jni.MpvNative
import io.github.yuroyami.libmpvkt.jni.MpvNativeApi
import io.github.yuroyami.libmpvkt.jni.MpvSurfaceReferences

/** Worker-thread native surface ownership. Retired slots are released after mpv is destroyed. */
@OptIn(MpvNativeApi::class)
internal class MpvSurfaceWorker(
    private val host: MpvSurfaceHost,
    private val onMounted: (MpvSurfaceSlot, Int) -> Unit,
) {
    private val slots = mutableListOf<MpvSurfaceSlot>()
    private var mounted: MpvSurfaceSlot? = null

    fun created(slot: MpvSurfaceSlot) {
        slots.add(slot)
        if (host.handle != 0L && !slot.retired) mount(host.handle, slot)
    }

    fun destroyed(slot: MpvSurfaceSlot) {
        if (mounted === slot && host.handle != 0L) unmount(host.handle)
        if (host.handle == 0L) releaseRetired()
    }

    fun mount(
        handle: Long,
        slot: MpvSurfaceSlot,
    ) {
        val token = host.token
        if (mounted != null) unmount(handle)
        val ref = MpvSurfaceReferences.create(slot.surface)
        slot.nativeReferences.add(ref)
        check(MpvNative.setOptionNode(handle, "wid", NodeCodec.encode(MpvNode.Int64(ref))) >= 0)
        MpvNative.setPropertyString(handle, "android-surface-size", "${slot.width}x${slot.height}")
        check(MpvNative.setPropertyString(handle, "vo", "gpu") >= 0)
        mounted = slot
        onMounted(slot, token)
    }

    /** A paused frame may already have been latched before the UI armed this surface. */
    fun redrawPaused(slot: MpvSurfaceSlot) {
        val handle = host.handle
        if (mounted !== slot || slot.retired || handle == 0L) return
        if (MpvNative.getPropertyString(handle, "pause") == "yes") {
            MpvNative.getPropertyString(handle, "time-pos")?.let {
                MpvNative.setPropertyString(handle, "time-pos", it)
            }
        }
    }

    fun engineDestroyed() {
        mounted = null
        releaseRetired()
    }

    private fun unmount(handle: Long) {
        MpvNative.setPropertyString(handle, "vo", "null")
        MpvNative.setOptionNode(handle, "wid", NodeCodec.encode(MpvNode.Int64(0)))
        mounted = null
        // Keep JNI references until terminateDestroy: mpv's renderer may still use them.
    }

    private fun releaseRetired() {
        val iterator = slots.iterator()
        while (iterator.hasNext()) {
            val slot = iterator.next()
            for (reference in slot.nativeReferences) MpvSurfaceReferences.release(reference)
            slot.nativeReferences.clear()
            if (slot.retired) {
                slot.release()
                iterator.remove()
            }
        }
    }
}
