package com.solthariel.pawchan.ffmpeg;

import java.io.File;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import org.junit.Test;
import static org.junit.Assert.*;

public class PublishedInputTimingTest {
    @Test public void waitingEndsOnPublicationWithoutChangingBytes() throws Exception { exercise(false); }
    @Test public void waitingEndsOnCancellationWithoutFabricatingBytes() throws Exception { exercise(true); }
    @Test public void localReadDoesNotReportNetworkWait() throws Exception {
        File file = File.createTempFile("input-timing", ".media");
        List<String> lines = new ArrayList<>();
        TimingProbe probe = new TimingProbe(true, System::nanoTime, lines::add);
        try (PublishedInput input = new PublishedInput(() -> {}, () -> {}, probe)) {
            Files.write(file.toPath(), new byte[] { 7 });
            input.open(file, null);
            byte[] target = new byte[1];
            assertEquals(1, input.read(0, target, 1)); assertEquals(7, target[0]);
            assertTrue(lines.isEmpty());
        } finally { assertTrue(file.delete()); }
    }
    private void exercise(boolean cancel) throws Exception {
        File file = File.createTempFile("input-timing", ".media");
        Files.write(file.toPath(), new byte[] { 7 });
        CountDownLatch requested = new CountDownLatch(1);
        List<String> lines = java.util.Collections.synchronizedList(new ArrayList<>());
        TimingProbe probe = new TimingProbe(true, System::nanoTime, lines::add);
        java.util.concurrent.ExecutorService worker = java.util.concurrent.Executors.newSingleThreadExecutor();
        try (PublishedInput input = new PublishedInput(() -> {}, () -> {}, probe)) {
            input.open(file, new PawPlayback.RangeCallback() {
                public void onReadReady() { input.publish(0, 1); }
                public void requestPartFromPosition(long position) { requested.countDown(); }
            });
            byte[] target = new byte[1];
            java.util.concurrent.Future<Integer> read = worker.submit(() -> input.read(0, target, 1));
            assertTrue(requested.await(2, TimeUnit.SECONDS));
            if (cancel) input.interrupt(); else input.publish(1, 1);
            assertEquals(cancel ? -2 : 1, (int) read.get(2, TimeUnit.SECONDS));
            assertEquals(cancel ? 0 : 7, target[0]);
            assertEquals(2, lines.size());
            assertTrue(lines.get(0).contains("event=READ_WAIT_START value=0"));
            assertTrue(lines.get(1).contains("event=READ_WAIT_END value="));
        } finally { worker.shutdownNow(); assertTrue(file.delete()); }
    }
}
