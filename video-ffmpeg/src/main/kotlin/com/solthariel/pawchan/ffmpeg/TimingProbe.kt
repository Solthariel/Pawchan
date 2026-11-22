package com.solthariel.pawchan.ffmpeg

import java.util.concurrent.atomic.AtomicInteger
import java.util.function.Consumer
import java.util.function.LongSupplier

/** Per-operation, opt-in timing. Only enum events and numbers can enter the log. */
class TimingProbe internal constructor(
    private val enabledValue: Boolean,
    private val clock: LongSupplier,
    private val sink: Consumer<String>,
) {
    enum class Event {
        OPEN,
        SOURCE_REUSED,
        INITIALIZE,
        INITIALIZED,
        FIRST_OUTPUT,
        STOP,
        GET_START,
        GET_HEADERS,
        GET_LENGTH,
        GET_FIRST_BYTES,
        GET_COMPLETE,
        GET_ERROR,
        SOURCE_CLOSE,
        RANGE_REQUEST,
        RANGE_NO_VALIDATOR,
        RANGE_UNAVAILABLE,
        RANGE_HEADERS,
        RANGE_ERROR,
        READ_WAIT_START,
        READ_WAIT_END,
        CONTAINER_END,
        STREAM_INFO_END,
        DECODER_OPEN_END,
        DURATION_SCAN_END,
        IMAGE_START,
        IMAGE_SIZE_START,
        IMAGE_SIZE_END,
        IMAGE_FETCH_START,
        IMAGE_FETCH_END,
        IMAGE_DECODE_START,
        IMAGE_DECODE_END,
        IMAGE_SUCCESS,
        IMAGE_ERROR,
        IMAGE_CANCEL,
    }

    private val count = AtomicInteger()
    private val started = if (enabledValue) clock.getAsLong() else 0L
    private val probeId = System.identityHashCode(this)

    fun enabled(): Boolean = enabledValue

    fun id(): Int = probeId

    fun now(): Long = if (enabledValue) clock.getAsLong() else 0L

    fun mark(
        event: Event,
        value: Long,
    ) {
        if (!enabledValue) return
        var slot = count.get()
        while (slot < LIMIT && !count.compareAndSet(slot, slot + 1)) slot = count.get()
        if (slot >= LIMIT) return
        val elapsed = maxOf(0L, clock.getAsLong() - started) / NANOS_PER_MILLI
        try {
            sink.accept("probe=$probeId elapsed_ms=$elapsed event=$event value=$value")
        } catch (_: RuntimeException) {
            // Diagnostic output must never turn a successful media operation into an error.
        }
    }

    @JvmName("nativeStage")
    internal fun nativeStage(
        stage: Int,
        micros: Long,
    ) {
        // Native stage numbers start at 1 and follow this order.
        val event = NATIVE_STAGES.getOrNull(stage - 1) ?: return
        mark(event, micros)
    }

    companion object {
        private const val LIMIT = 128
        private const val NANOS_PER_MILLI = 1_000_000L
        private val NATIVE_STAGES =
            listOf(Event.CONTAINER_END, Event.STREAM_INFO_END, Event.DECODER_OPEN_END, Event.DURATION_SCAN_END)

        @JvmStatic
        fun disabled(): TimingProbe =
            TimingProbe(
                false,
                LongSupplier { System.nanoTime() },
                Consumer { },
            )
    }
}
