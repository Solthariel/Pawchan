package com.solthariel.pawchan.ffmpeg

import java.io.EOFException
import java.io.File
import java.io.IOException
import java.io.RandomAccessFile
import java.util.concurrent.TimeUnit
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

/** A single native reader; range publication is synchronized, disk reads stay on the decode thread. */
internal class PublishedInput(
    private val waiting: Runnable,
    private val resumed: Runnable,
    private val timing: TimingProbe,
) : ProgressiveInputSink,
    AutoCloseable {
    constructor(waiting: Runnable, resumed: Runnable) : this(waiting, resumed, TimingProbe.disabled())

    private val lock = ReentrantLock()
    private val changed = lock.newCondition()
    private var file: RandomAccessFile? = null
    private var callback: ProgressiveReadCallback? = null
    private var prefix = 0L
    private var total = -1L
    private var tailStart = -1L
    private var tailEnd = -1L

    @Volatile var interrupted = false
        private set

    @Volatile var failed = false
        private set

    /** Seek cancellation is temporary and remains set until the decode thread consumes the seek. */
    @Volatile var readCancelled = false
        set(value) {
            lock.withLock {
                field = value
                changed.signalAll()
            }
        }

    @Throws(IOException::class)
    fun open(
        target: File,
        callback: ProgressiveReadCallback?,
    ) {
        val opened = RandomAccessFile(target, "r")
        file = opened
        this.callback = callback
        if (callback == null) publish(opened.length(), opened.length()) else callback.onReadReady()
    }

    override fun publish(
        prefix: Long,
        total: Long,
    ) {
        lock.withLock {
            this.prefix = maxOf(this.prefix, prefix)
            this.total = total
            changed.signalAll()
        }
    }

    override fun publishTail(
        start: Long,
        end: Long,
    ) {
        lock.withLock {
            tailStart = start
            tailEnd = end
            changed.signalAll()
        }
    }

    fun length(): Long = lock.withLock { total }

    fun complete(): Boolean = lock.withLock { total > 0 && prefix >= total }

    /** JNI: -1 EOF, -2 permanent cancellation/error, -3 read superseded by a seek. */
    fun read(
        position: Long,
        target: ByteArray,
        maximum: Int,
    ): Int = read(position, target, maximum, 0)

    fun read(
        position: Long,
        target: ByteArray,
        maximum: Int,
        offset: Int,
    ): Int {
        val wait = ReadWait(position)
        var result = CANCELLED
        try {
            while (!interrupted && !readCancelled) {
                result = readPublished(position, target, maximum, offset)
                if (result != NOTHING_PUBLISHED) break
                result = CANCELLED
                wait.await()
            }
        } catch (_: IOException) {
            failed = true
            result = CANCELLED
        } catch (_: InterruptedException) {
            Thread.currentThread().interrupt()
            result = CANCELLED
        } finally {
            wait.finish()
        }
        return if (!interrupted && readCancelled) SEEK_CANCELLED else result
    }

    private fun readPublished(
        position: Long,
        target: ByteArray,
        maximum: Int,
        offset: Int,
    ): Int =
        lock.withLock {
            val available = PublishedRanges.available(position, prefix, tailStart, tailEnd, total)
            when {
                available < 0 -> {
                    available.toInt()
                }

                available == 0L -> {
                    NOTHING_PUBLISHED
                }

                else -> {
                    val source = checkNotNull(file)
                    source.seek(position)
                    val length = minOf(available, minOf(target.size - offset, maximum).toLong()).toInt()
                    val read = source.read(target, offset, length)
                    if (read < 0) throw EOFException("Published video bytes are missing")
                    read
                }
            }
        }

    fun interrupt() {
        // mpv may call cancel from a separate thread; it must never wait for disk I/O.
        interrupted = true
        if (lock.tryLock()) {
            try {
                changed.signalAll()
            } finally {
                lock.unlock()
            }
        }
        // If the lock is busy, ReadWait checks interrupted at most 50 ms later.
    }

    @Throws(IOException::class)
    override fun close() {
        file?.close()
    }

    /** Reports one network wait per read and requests the missing range only once. */
    private inner class ReadWait(
        private val position: Long,
    ) {
        private var busy = false
        private var requested = false
        private var started = 0L

        fun await() {
            if (!busy) {
                busy = true
                started = timing.now()
                timing.mark(TimingProbe.Event.READ_WAIT_START, position)
                waiting.run()
            }
            val active = callback
            if (!requested && active != null) {
                requested = true
                active.requestPartFromPosition(position)
            }
            lock.withLock {
                if (!interrupted && !readCancelled) changed.await(WAIT_SLICE_MILLIS, TimeUnit.MILLISECONDS)
            }
        }

        fun finish() {
            if (busy) {
                timing.mark(TimingProbe.Event.READ_WAIT_END, (timing.now() - started) / NANOS_PER_MICRO)
                resumed.run()
            }
        }
    }

    private companion object {
        const val CANCELLED = -2
        const val SEEK_CANCELLED = -3
        const val NOTHING_PUBLISHED = Int.MIN_VALUE
        const val WAIT_SLICE_MILLIS = 50L
        const val NANOS_PER_MICRO = 1000L
    }
}
