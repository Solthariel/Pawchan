package com.solthariel.pawchan.ffmpeg;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.function.LongSupplier;

/** Per-operation, opt-in timing. Only enum events and numbers can enter the log. */
public final class TimingProbe {
    public enum Event {
        OPEN, SOURCE_REUSED, INITIALIZE, INITIALIZED, FIRST_OUTPUT, STOP,
        GET_START, GET_HEADERS, GET_LENGTH, GET_FIRST_BYTES, GET_COMPLETE, GET_ERROR, SOURCE_CLOSE,
        RANGE_REQUEST, RANGE_NO_VALIDATOR, RANGE_UNAVAILABLE, RANGE_HEADERS, RANGE_ERROR,
        READ_WAIT_START, READ_WAIT_END,
        CONTAINER_END, STREAM_INFO_END, DECODER_OPEN_END, DURATION_SCAN_END,
        IMAGE_START, IMAGE_SIZE_START, IMAGE_SIZE_END, IMAGE_FETCH_START, IMAGE_FETCH_END,
        IMAGE_DECODE_START, IMAGE_DECODE_END, IMAGE_SUCCESS, IMAGE_ERROR, IMAGE_CANCEL
    }
    private static final int LIMIT = 128;
    private final boolean enabled;
    private final LongSupplier clock;
    private final Consumer<String> sink;
    private final AtomicInteger count = new AtomicInteger();
    private final long started;
    private final int id = System.identityHashCode(this);

    TimingProbe(boolean enabled, LongSupplier clock, Consumer<String> sink) {
        this.enabled = enabled;
        this.clock = clock;
        this.sink = sink;
        started = enabled ? clock.getAsLong() : 0;
    }
    public static TimingProbe disabled() { return new TimingProbe(false, System::nanoTime, line -> { }); }
    public boolean enabled() { return enabled; }
    public int id() { return id; }
    public long now() { return enabled ? clock.getAsLong() : 0; }
    public void mark(Event event, long value) {
        if (!enabled) return;
        int slot = count.get();
        while (slot < LIMIT && !count.compareAndSet(slot, slot + 1)) slot = count.get();
        if (slot >= LIMIT) return;
        long elapsed = Math.max(0, clock.getAsLong() - started) / 1000000;
        try {
            sink.accept("probe=" + id + " elapsed_ms=" + elapsed + " event=" + event + " value=" + value);
        } catch (RuntimeException unavailable) {
            // Diagnostic output must never turn a successful media operation into an error.
        }
    }
    void nativeStage(int stage, long micros) {
        Event event;
        switch (stage) {
            case 1: event = Event.CONTAINER_END; break;
            case 2: event = Event.STREAM_INFO_END; break;
            case 3: event = Event.DECODER_OPEN_END; break;
            case 4: event = Event.DURATION_SCAN_END; break;
            default: return;
        }
        mark(event, micros);
    }
}
