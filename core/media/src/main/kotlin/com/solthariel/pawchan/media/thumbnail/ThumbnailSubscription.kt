package com.solthariel.pawchan.media.thumbnail

import android.graphics.Bitmap
import androidx.compose.runtime.RememberObserver
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

internal class ThumbnailSubscription(
    private val owner: ThumbnailLoader,
    internal val url: String,
) : RememberObserver {
    private val mutableBitmap = MutableStateFlow<Bitmap?>(null)
    val bitmap: StateFlow<Bitmap?> = mutableBitmap
    internal val failed = MutableStateFlow(false)
    internal var instantly = false
        private set
    internal var task: ThumbnailTask? = null

    internal fun deliver(
        bitmap: Bitmap?,
        instantly: Boolean = false,
    ) {
        this.instantly = instantly
        mutableBitmap.value = bitmap
        failed.value = bitmap == null
    }

    override fun onRemembered() = owner.attach(this)

    override fun onForgotten() = owner.detach(this)

    override fun onAbandoned() = owner.detach(this)
}

internal class ThumbnailTask(
    val key: String,
    val url: String,
) {
    val targets = LinkedHashSet<ThumbnailSubscription>()
    var job: Job? = null
    var finished = false
    var bitmap: Bitmap? = null
    var startGate: ThumbnailStartGate? = null

    val ready: Boolean
        get() = startGate?.ready?.value == true
}

internal data class ThumbnailResult(
    val bitmap: Bitmap? = null,
    val failureMillis: Long = TRANSIENT_FAILURE_MILLIS,
)
