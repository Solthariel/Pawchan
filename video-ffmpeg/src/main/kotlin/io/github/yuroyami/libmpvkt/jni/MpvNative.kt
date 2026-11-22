package io.github.yuroyami.libmpvkt.jni

import android.content.Context
import io.github.yuroyami.libmpvkt.stream.MpvStreamProvider

/** Raw libmpv calls used by Pawchan. A session owns each handle and serializes its calls. */
@MpvNativeApi
public object MpvNative {
    init {
        System.loadLibrary("mpv")
        System.loadLibrary("mpvkt_jni")
    }

    public external fun initAndroid(appContext: Context)

    public external fun create(): Long

    public external fun initialize(handle: Long): Int

    public external fun terminateDestroy(handle: Long)

    public external fun setOptionString(
        handle: Long,
        name: String,
        value: String,
    ): Int

    public external fun setOptionNode(
        handle: Long,
        name: String,
        node: ByteArray,
    ): Int

    public external fun command(
        handle: Long,
        args: Array<String>,
    ): Int

    public external fun getPropertyString(
        handle: Long,
        name: String,
    ): String?

    public external fun setPropertyString(
        handle: Long,
        name: String,
        value: String,
    ): Int

    public external fun waitEvent(
        handle: Long,
        timeoutSeconds: Double,
    ): ByteArray

    public external fun streamCbAddRo(
        handle: Long,
        protocol: String,
        provider: MpvStreamProvider,
    ): Int
}
