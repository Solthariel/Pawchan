package com.solthariel.pawchan.ffmpeg;
import static org.junit.Assert.*;
import java.io.File;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;
import java.util.function.BooleanSupplier;
import org.junit.Test;
/** Original malformed WebM, no remux. Requires host FFmpeg VP8/Vorbis. */
public final class IncompleteInputHostTest {
    static { System.load(System.getProperty("paw.hostLibrary")); }
    @Test public void missingTailIsNotSuccessfulCompletionAndRemainsSeekable() throws Exception {
        try (Session s = new Session()) {
            assertTrue("Duration must be corrected before playback",
                    s.status()[1] >= 12900 && s.status()[1] <= 13100);
            assertEquals("Probe must not advance playback", 0, s.status()[0]);
            s.command(PawNative.SEEK, 40000); s.command(PawNative.PLAY, 1);
            assertEquals(2, (int) s.result.get(5, TimeUnit.SECONDS));
            assertTrue(s.status()[1] >= 12900 && s.status()[1] <= 13100);
            assertTrue("Fallback must be actual last video frame", s.status()[0] >= 12800 && s.status()[0] < 13000);
            long held = s.status()[0]; Thread.sleep(150); assertEquals(held, s.status()[0]);
            s.command(PawNative.SEEK, 5000);
            await(() -> s.status()[0] >= 5000 && s.status()[0] <= 5100);
            Thread.sleep(100); assertTrue(s.status()[0] <= 5100);
            s.command(PawNative.PLAY, 1); await(() -> s.status()[0] >= 5300);
        }
    }
    @Test public void pausedSeekMustActuallyDecodeBeforeMovingProgress() throws Exception {
        try (Session s = new Session()) {
            s.command(PawNative.SEEK, 10000);
            await(() -> s.status()[0] >= 10000 && s.status()[0] <= 10100);
            Thread.sleep(120); assertTrue(s.status()[0] <= 10100);
            assertFalse("Paused seek emitted completion", s.result.isDone());
        }
    }
    @Test public void naturalPrematureEndAlsoDoesNotRequestAutoRepeat() throws Exception {
        try (Session s = new Session()) {
            s.command(PawNative.SPEED, 2000); s.command(PawNative.PLAY, 1);
            assertEquals(2, (int) s.result.get(12, TimeUnit.SECONDS));
            assertTrue(s.status()[1] >= 12900 && s.status()[1] <= 13100);
            assertTrue(s.status()[0] >= 12800);
        }
    }
    @Test public void unpublishedPrefixDefersProbeUntilWholeFileIsPublished() throws Exception {
        try (Session s = new Session(true)) {
            assertEquals("Unverified duration must not leak to the UI", 0, s.status()[1]);
            s.command(PawNative.PLAY, 1);
            await(() -> s.status()[0] >= 150);
            assertEquals("Playback must not expose the provisional 49 seconds", 0, s.status()[1]);
            long total = new File(System.getProperty("paw.incompleteFile")).length();
            s.input.publish(total, total);
            s.command(PawNative.PLAY, 1);
            await(() -> s.status()[1] >= 12900 && s.status()[1] <= 13100);
            assertTrue("Correction must not wait until playback EOF", s.status()[0] < 1000);
        }
    }
    @Test public void ineligibleLargeFileDoesNotLoseDurationWhileDownloading() throws Exception {
        File source = new File(System.getProperty("paw.incompleteFile"));
        File large = File.createTempFile("paw-large-duration", ".webm");
        try {
            java.nio.file.Files.copy(source.toPath(), large.toPath(), java.nio.file.StandardCopyOption.REPLACE_EXISTING);
            try (java.io.RandomAccessFile expanded = new java.io.RandomAccessFile(large, "rw")) {
                expanded.setLength(16L * 1024 * 1024 + 1);
            }
            try (Session s = new Session(true, large)) {
                assertEquals("No pending probe for a file exceeding the scan limit", 49894, s.status()[1]);
            }
        } finally { large.delete(); }
    }
    private static void await(BooleanSupplier condition) throws Exception {
        long until = System.nanoTime() + TimeUnit.SECONDS.toNanos(5);
        while (!condition.getAsBoolean() && System.nanoTime() < until) Thread.sleep(5);
        assertTrue("Actual rendered position not reached", condition.getAsBoolean());
    }
    private static final class Session implements AutoCloseable {
        final PublishedInput input = new PublishedInput(() -> {}, () -> {});
        final PcmOutput audio = new PcmOutput();
        final AtomicLong handle = new AtomicLong();
        final CompletableFuture<Void> opened = new CompletableFuture<>();
        final CompletableFuture<Integer> result = new CompletableFuture<>();
        final Thread thread;
        Session() throws Exception { this(false); }
        Session(boolean partial) throws Exception {
            this(partial, new File(System.getProperty("paw.incompleteFile")));
        }
        Session(boolean partial, File file) throws Exception {
            if (partial) {
                input.open(file, new PawPlayback.RangeCallback() {
                    @Override public void onReadReady() {}
                    @Override public void requestPartFromPosition(long position) {}
                });
                input.publish(file.length() - 1, file.length());
                input.publishTail(file.length() - 1, file.length());
            } else input.open(file, null);
            thread = new Thread(() -> {
                long id = PawNative.create(); handle.set(id);
                try {
                    if (!PawNative.open(id, input, audio, true)) throw new IllegalStateException("Open failed");
                    opened.complete(null);
                    while (!input.isInterrupted()) {
                        int code = PawNative.pump(id);
                        if (code < 0) throw new IllegalStateException("Decode failed");
                        if (code > 0) result.complete(code);
                    }
                } catch (Throwable error) { opened.completeExceptionally(error); result.completeExceptionally(error); }
                finally { synchronized (handle) { handle.set(0); PawNative.destroy(id); } }
            }, "paw-incomplete-test");
            thread.start();
            try { opened.get(5, TimeUnit.SECONDS); } catch (Exception error) { close(); throw error; }
        }
        long[] status() { synchronized (handle) { return handle.get() == 0 ? new long[6] : PawNative.status(handle.get()); } }
        void command(int type, long value) { synchronized (handle) { if (handle.get() != 0) PawNative.command(handle.get(), type, value); } }
        @Override public void close() throws Exception {
            input.interrupt(); audio.cancel(); command(PawNative.INTERRUPT, 0); thread.join(3000);
            assertFalse("Thread leaked", thread.isAlive()); input.close();
        }
    }
}
