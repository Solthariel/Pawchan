package com.solthariel.pawchan.media.thumbnail

import android.app.ActivityManager
import android.content.Context
import android.graphics.Bitmap
import android.os.Looper
import android.os.Process
import android.os.SystemClock
import android.util.Log
import android.util.LruCache
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import java.io.IOException
import java.security.MessageDigest
import java.util.concurrent.Executors

/** Registry and subscriptions are confined to Main; decoding/cache I/O belongs to workers. */
class ThumbnailLoader(
    context: Context,
    baseHttpClient: OkHttpClient,
    diskBytes: Long = DISK_BYTES,
    diskEnabled: Boolean = true,
) : AutoCloseable {
    private val mainScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val executor =
        Executors.newFixedThreadPool(LOADER_THREAD_COUNT) { runnable ->
            Thread({
                Process.setThreadPriority(Process.THREAD_PRIORITY_BACKGROUND)
                runnable.run()
            }, "PawchanThumbnailDecode").apply { isDaemon = true }
        }
    private val dispatcher = executor.asCoroutineDispatcher()
    private val disk =
        if (diskEnabled) {
            ThumbnailDiskCache(context.cacheDir.resolve("thumbnail_cache"), diskBytes)
        } else {
            null
        }
    private val memory = thumbnailMemoryCache(context)
    private val pipeline = ThumbnailPipeline(baseHttpClient, disk, dispatcher, context.resources.displayMetrics.density)
    private val active = LinkedHashSet<ThumbnailSubscription>()
    private val tasks = HashMap<String, ThumbnailTask>()
    private val failures = LruCache<String, Long>(MAX_FAILURE_KEYS)
    private val keys = LruCache<String, String>(MAX_FAILURE_KEYS)

    internal fun subscription(url: String): ThumbnailSubscription {
        checkMainThread()
        return ThumbnailSubscription(this, url)
    }

    /** Уже декодированная миниатюра из памяти, без загрузки; null — её там нет. */
    fun cached(url: String): Bitmap? {
        checkMainThread()
        return memory[cacheKey(url)]
    }

    internal fun attach(target: ThumbnailSubscription) {
        checkMainThread()
        active += target
        if (target.task == null) {
            val key = cacheKey(target.url)
            val cached = memory[key]
            val current = tasks[key]
            when {
                cached != null -> {
                    target.deliver(cached, instantly = true)
                }

                current != null -> {
                    attachToTask(target, current)
                }

                (failures[key] ?: 0L) > SystemClock.elapsedRealtime() -> {
                    target.deliver(null)
                }

                else -> {
                    val task = ThumbnailTask(key, target.url)
                    tasks[key] = task
                    attachToTask(target, task)
                    start(task)
                }
            }
        }
    }

    private fun attachToTask(
        target: ThumbnailSubscription,
        task: ThumbnailTask,
    ) {
        task.targets += target
        target.task = task
        if (task.finished) target.deliver(task.bitmap, instantly = true)
    }

    internal fun detach(target: ThumbnailSubscription) {
        checkMainThread()
        active -= target
        val task = target.task ?: return
        target.task = null
        task.targets -= target
        if (task.targets.isEmpty()) {
            task.job?.cancel()
            task.startGate?.close()
            if (tasks[task.key] === task) tasks.remove(task.key)
        }
    }

    private fun start(task: ThumbnailTask) {
        if (task.startGate == null) task.startGate = ThumbnailStartGate(mainScope)
        task.job =
            mainScope.launch {
                val result =
                    try {
                        pipeline.load(task)
                    } catch (cancellation: CancellationException) {
                        throw cancellation
                    } catch (error: OutOfMemoryError) {
                        Log.e(LOG_TAG, "Thumbnail decode exceeded memory budget", error)
                        ThumbnailResult()
                    } catch (exception: IOException) {
                        thumbnailFailure(task.url, exception)
                    } catch (exception: IllegalArgumentException) {
                        thumbnailFailure(task.url, exception)
                    }
                complete(task, result)
            }
    }

    private fun complete(
        task: ThumbnailTask,
        result: ThumbnailResult,
    ) {
        if (tasks[task.key] !== task) return
        task.finished = true
        task.bitmap = result.bitmap
        if (result.bitmap != null) {
            memory.put(task.key, result.bitmap)
            failures.remove(task.key)
        } else {
            failures.put(task.key, SystemClock.elapsedRealtime() + result.failureMillis)
        }
        task.targets.forEach { it.deliver(result.bitmap) }
    }

    /** Manual refresh only retries failed active subscriptions, never pending jobs or server cooldown. */
    fun retryVisible() {
        checkMainThread()
        val retry = tasks.values.filter { it.finished && it.bitmap == null }.toList()
        retry.forEach { task ->
            failures.remove(task.key)
            task.finished = false
            task.targets.forEach { it.failed.value = false }
            start(task)
        }
        active.filter { it.failed.value && it.task == null }.toList().forEach { target ->
            failures.remove(cacheKey(target.url))
            target.failed.value = false
            attach(target)
        }
    }

    private fun cacheKey(url: String): String {
        keys[url]?.let { return it }
        val digest = MessageDigest.getInstance("SHA-256").digest(url.toByteArray(Charsets.UTF_8))
        val characters = CharArray(digest.size * 2)
        digest.forEachIndexed { index, byte ->
            val value = byte.toInt() and BYTE_MASK
            characters[index * 2] = HEX[value ushr HEX_SHIFT]
            characters[index * 2 + 1] = HEX[value and HALF_BYTE_MASK]
        }
        return String(characters).also { keys.put(url, it) }
    }

    suspend fun clearCache() {
        checkMainThread()
        memory.clear()
        kotlinx.coroutines.withContext(dispatcher) { disk?.clear() }
    }

    override fun close() {
        checkMainThread()
        tasks.values.forEach {
            it.job?.cancel()
            it.startGate?.close()
        }
        tasks.clear()
        active.clear()
        mainScope.cancel()
        dispatcher.close()
        executor.shutdownNow()
        disk?.close()
        memory.clear()
    }
}

private fun checkMainThread() {
    check(Looper.myLooper() == Looper.getMainLooper()) { "Thumbnail subscriptions must run on Main" }
}

private fun thumbnailMemoryCache(context: Context): ThumbnailMemoryCache {
    val manager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
    return ThumbnailMemoryCache(
        maxEntries = if (manager.isLowRamDevice) LOW_RAM_ENTRIES else NORMAL_ENTRIES,
        maxSizeBytes = if (manager.isLowRamDevice) LOW_RAM_BYTES else NORMAL_BYTES,
    )
}

internal const val TRANSIENT_FAILURE_MILLIS = 30_000L
internal const val MISSING_THUMBNAIL_MILLIS = 300_000L
private const val LOADER_THREAD_COUNT = 3
private const val DISK_BYTES = 2L * 1024L * 1024L * 1024L
private const val LOW_RAM_ENTRIES = 50
private const val NORMAL_ENTRIES = 200
private const val LOW_RAM_BYTES = 16L * 1024L * 1024L
private const val NORMAL_BYTES = 64L * 1024L * 1024L
private const val MAX_FAILURE_KEYS = 1_000
private const val BYTE_MASK = 0xff
private const val HALF_BYTE_MASK = 0x0f
private const val LOG_TAG = "PawchanThumbnail"
private val HEX = "0123456789abcdef".toCharArray()

private const val HEX_SHIFT = 4
