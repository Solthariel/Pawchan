package com.solthariel.pawchan.ffmpeg;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.Test;
import static org.junit.Assert.*;

public class TimingProbeTest {
    @Test public void disabledDoesNotReadClockOrWriteSink() {
        TimingProbe probe = new TimingProbe(false, () -> { throw new AssertionError("clock"); },
                line -> { throw new AssertionError("sink"); });
        assertFalse(probe.enabled());
        assertEquals(0, probe.now());
        probe.mark(TimingProbe.Event.OPEN, 1);
        probe.nativeStage(4, 500000);
    }
    @Test public void recordsMonotonicElapsedAndNumericValue() {
        AtomicLong clock = new AtomicLong(1000000000);
        List<String> lines = new ArrayList<>();
        TimingProbe probe = new TimingProbe(true, clock::get, lines::add);
        clock.addAndGet(125000000);
        probe.mark(TimingProbe.Event.GET_HEADERS, 206);
        assertEquals("probe=" + probe.id() + " elapsed_ms=125 event=GET_HEADERS value=206", lines.get(0));
    }
    @Test public void boundedEvenWithConcurrentWriters() throws Exception {
        List<String> lines = Collections.synchronizedList(new ArrayList<>());
        TimingProbe probe = new TimingProbe(true, System::nanoTime, lines::add);
        List<Thread> writers = new ArrayList<>();
        for (int i = 0; i < 8; i++) {
            Thread thread = new Thread(() -> {
                for (int n = 0; n < 1000; n++) probe.mark(TimingProbe.Event.READ_WAIT_END, n);
            });
            writers.add(thread); thread.start();
        }
        for (Thread writer : writers) writer.join();
        assertEquals(128, lines.size());
        probe.mark(TimingProbe.Event.OPEN, 0);
        assertEquals(128, lines.size());
    }
    @Test public void nativeStagesKeepMicrosecondsAndIgnoreUnknownCodes() {
        List<String> lines = new ArrayList<>();
        TimingProbe probe = new TimingProbe(true, () -> 1, lines::add);
        probe.nativeStage(1, 10); probe.nativeStage(2, 20);
        probe.nativeStage(3, 30); probe.nativeStage(4, 40);
        probe.nativeStage(99, 50);
        assertEquals(4, lines.size());
        assertTrue(lines.get(0).endsWith("event=CONTAINER_END value=10"));
        assertTrue(lines.get(1).endsWith("event=STREAM_INFO_END value=20"));
        assertTrue(lines.get(2).endsWith("event=DECODER_OPEN_END value=30"));
        assertTrue(lines.get(3).endsWith("event=DURATION_SCAN_END value=40"));
    }
    @Test public void failingSinkDoesNotFailPlayback() {
        TimingProbe probe = new TimingProbe(true, () -> 1, line -> { throw new IllegalStateException("unavailable"); });
        probe.mark(TimingProbe.Event.FIRST_OUTPUT, 0);
        probe.nativeStage(4, 123);
    }
    @Test public void disabledFactoryRequiresNoAndroidRuntime() {
        TimingProbe probe = TimingProbe.disabled();
        probe.mark(TimingProbe.Event.IMAGE_START, 0);
        assertFalse(probe.enabled());
    }
}
