package com.solthariel.pawchan.thread

import android.graphics.Bitmap
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.layer.GraphicsLayer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Job
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.launch
import java.io.IOException

/** Main-thread, screen-owned capture. Tap freezes the clean row before selection tint or a file picker appears. */
internal class ThreadShotCapture(
    private val scope: CoroutineScope,
) {
    private val selection = ShotSelection<Bitmap>()
    private val visible = mutableMapOf<Long, ShotLayer>()
    private val pending = mutableMapOf<Long, Job>()
    private var requested = emptySet<Long>()
    var ready by mutableStateOf(false)
        private set

    fun recorded(
        id: Long,
        layer: GraphicsLayer,
    ) {
        val size = layer.size
        val current = visible[id]
        if (current == null || !current.matches(layer, size.width, size.height)) {
            visible[id] = ShotLayer(layer, ShotGeometry(size.width, size.height))
        }
    }

    fun detach(
        id: Long,
        layer: GraphicsLayer,
    ) {
        if (visible[id]?.layer === layer) visible.remove(id)
    }

    fun deselect(id: Long) {
        pending.remove(id)?.cancel()
        selection.remove(id)
        requested -= id
        ready = selection.isReady(requested)
    }

    fun request(
        id: Long,
        onFailure: (String) -> Unit,
    ) {
        requested += id
        ready = false
        val row = visible[id]
        if (row == null) onFailure(SHOT_MISSING) else capture(id, row, onFailure)
    }

    private fun capture(
        id: Long,
        row: ShotLayer,
        onFailure: (String) -> Unit,
    ) {
        try {
            selection.select(id, row.geometry)
        } catch (error: IOException) {
            onFailure(error.message ?: SHOT_TOO_LARGE)
            return
        }
        val job =
            scope.launch(start = CoroutineStart.UNDISPATCHED) {
                try {
                    val image = row.pixels()
                    selection.store(id, row.geometry, image)
                } catch (cancelled: kotlinx.coroutines.CancellationException) {
                    throw cancelled
                } catch (_: IllegalStateException) {
                    failed(id, onFailure)
                } catch (_: IllegalArgumentException) {
                    failed(id, onFailure)
                } catch (_: IOException) {
                    failed(id, onFailure)
                } finally {
                    ready = selection.isReady(requested)
                }
            }
        pending[id] = job
        job.invokeOnCompletion { if (pending[id] === job) pending.remove(id) }
    }

    private fun failed(
        id: Long,
        onFailure: (String) -> Unit,
    ) {
        selection.remove(id)
        onFailure(SHOT_MISSING)
    }

    suspend fun freeze(displayOrder: List<Long>): CapturedThreadShot {
        val ids = requested
        if (!selection.isReady(ids)) throw IOException(SHOT_MISSING)
        // A visible thumbnail may have arrived since selection. A scrolled-away row keeps its captured pixels.
        for (id in displayOrder) {
            val row = visible[id]?.takeIf { id in ids } ?: continue
            selection.select(id, row.geometry)
            val image = row.pixels()
            if (requested != ids) throw IOException(SHOT_MISSING)
            selection.store(id, row.geometry, image)
        }
        return CapturedThreadShot(selection.snapshot(ids, displayOrder))
    }

    fun clear() {
        val jobs = pending.values.toList()
        pending.clear()
        jobs.forEach(Job::cancel)
        selection.clear()
        requested = emptySet()
        ready = false
    }
}

private class ShotLayer(
    val layer: GraphicsLayer,
    val geometry: ShotGeometry,
) {
    suspend fun pixels(): Bitmap {
        val image = layer.toImageBitmap().asAndroidBitmap()
        currentCoroutineContext().ensureActive()
        if (!matches(layer, image.width, image.height)) throw IOException(SHOT_MISSING)
        return image
    }

    fun matches(
        candidate: GraphicsLayer,
        width: Int,
        height: Int,
    ): Boolean = layer === candidate && geometry.width == width && geometry.height == height
}
