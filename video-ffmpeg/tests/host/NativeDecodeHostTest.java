package com.solthariel.pawchan.ffmpeg;
import static org.junit.Assert.*;
import java.io.File;
import java.nio.file.Files;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;
import java.util.function.BooleanSupplier;
import org.junit.Test;
/** Real FFmpeg and real JNI/IO/decoder, but NO Android surface, AudioTrack or MediaCodec runtime. */
public final class NativeDecodeHostTest {
    static { System.load(System.getProperty("paw.hostLibrary")); }
    private static File fixture(String name) { return new File(System.getProperty("paw.fixtures"), name); }
    @Test public void h264AacDemuxDecodeAndDrain() throws Exception { decode("fast.mp4", true); }
    @Test public void tailMoovIsSeekable() throws Exception { decode("tail.mp4", true); }
    @Test public void vp9OpusDemuxDecodeAndDrain() throws Exception { decode("sample.webm", true); }
    @Test public void ffv1PcmDemuxDecodeAndDrain() throws Exception { decode("sample.mkv", true); }
    @Test public void silentVideoDoesNotInventAudio() throws Exception { decode("silent.mp4", false); }
    @Test public void utf8MetadataAndDisplayRotationArePreserved() throws Exception {
        try (Session session = new Session(fixture("rotated.mp4"))) {
            String[] values;
            synchronized (session.handle) { values = PawNative.metadata(session.handle.get()); }
            java.util.Map<String,String> metadata = new java.util.HashMap<>();
            for (int i = 0; i + 1 < values.length; i += 2) metadata.put(values[i], values[i+1]);
            assertEquals("Привет 🐾", metadata.get("title"));
            assertEquals("64", metadata.get("width")); assertEquals("96", metadata.get("height"));
        }
    }
    private void decode(String file, boolean hasAudio) throws Exception {
        try (Session session = new Session(fixture(file))) {
            assertTrue(session.duration() > 500); session.command(PawNative.PLAY, 1);
            assertEquals(1, (int) session.ended.get(4, TimeUnit.SECONDS));
            assertTrue(session.position() >= 700);
            assertEquals(hasAudio, session.audio.bytes > 0);
        }
    }
    @Test public void pauseFreezesAndSeekWhilePausedShowsTarget() throws Exception {
        try (Session session = new Session(fixture("fast.mp4"))) {
            session.command(PawNative.PLAY, 1); await(() -> session.position() >= 150);
            session.command(PawNative.PLAY, 0); long frozen = session.position(); Thread.sleep(150);
            assertEquals(frozen, session.position());
            session.command(PawNative.SEEK, 700); await(() -> session.position() >= 700);
            long sought = session.position(); Thread.sleep(100); assertEquals(sought, session.position());
            session.command(PawNative.PLAY, 1); assertEquals(1, (int) session.ended.get(4, TimeUnit.SECONDS));
        }
    }
    @Test public void seekGenerationInvalidatesOldCompletion() throws Exception {
        try (Session session = new Session(fixture("fast.mp4"))) {
            session.command(PawNative.SEEK, 1000); session.command(PawNative.PLAY, 1);
            assertEquals(1, (int) session.ended.get(4, TimeUnit.SECONDS));
            long completed = session.status()[5]; session.command(PawNative.SEEK, 100);
            assertTrue(session.status()[4] > completed);
        }
    }
    @Test public void speedChangeKeepsTimelineContinuous() throws Exception {
        try (Session session = new Session(fixture("fast.mp4"))) {
            session.command(PawNative.PLAY, 1); await(() -> session.position() >= 100);
            long before = session.position(); session.command(PawNative.SPEED, 2000);
            assertTrue(Math.abs(session.position() - before) < 150);
            assertEquals(1, (int) session.ended.get(3, TimeUnit.SECONDS));
        }
    }
    @Test public void repeatedOpenCancelAndDestroy() throws Exception {
        for (int i = 0; i < 12; i++) try (Session session = new Session(fixture("fast.mp4"))) {
            session.command(PawNative.PLAY, 1);
        }
    }
    @Test public void malformedFileFailsWithoutNativeCrash() throws Exception {
        File bad = File.createTempFile("paw-invalid", ".media"); Files.write(bad.toPath(), new byte[]{1,2,3});
        try { new Session(bad).close(); fail("Invalid media accepted"); }
        catch (ExecutionException expected) { assertNotNull(expected.getCause()); }
        finally { bad.delete(); }
    }
    private static void await(BooleanSupplier condition) throws Exception {
        long until = System.nanoTime() + TimeUnit.SECONDS.toNanos(3);
        while (!condition.getAsBoolean() && System.nanoTime() < until) Thread.sleep(5);
        assertTrue("Native operation timed out", condition.getAsBoolean());
    }
    private static final class Session implements AutoCloseable {
        final PublishedInput input = new PublishedInput(() -> {}, () -> {});
        final PcmOutput audio = new PcmOutput();
        final AtomicLong handle = new AtomicLong();
        final CompletableFuture<Void> opened = new CompletableFuture<>();
        final CompletableFuture<Integer> ended = new CompletableFuture<>();
        final AtomicReference<Throwable> failure = new AtomicReference<>();
        final Thread thread;
        Session(File file) throws Exception {
            input.open(file, null);
            thread = new Thread(() -> {
                long id = PawNative.create(); handle.set(id);
                try {
                    if (!PawNative.open(id, input, audio, true)) throw new IllegalStateException("Open failed");
                    opened.complete(null);
                    while (!input.isInterrupted()) {
                        int result = PawNative.pump(id);
                        if (result < 0) throw new IllegalStateException("Decode failed");
                        if (result == 1) ended.complete(1);
                    }
                } catch (Throwable error) { failure.set(error); opened.completeExceptionally(error); ended.completeExceptionally(error); }
                finally { synchronized (handle) { handle.set(0); PawNative.destroy(id); } }
            }, "paw-host-decoder");
            thread.start();
            try { opened.get(4, TimeUnit.SECONDS); }
            catch (Exception error) { close(); throw error; }
        }
        void command(int command, long value) { synchronized (handle) { if (handle.get() != 0) PawNative.command(handle.get(), command, value); } }
        long[] status() { synchronized (handle) { return handle.get() == 0 ? new long[6] : PawNative.status(handle.get()); } }
        long duration() { return status()[1]; }
        long position() { return status()[0]; }
        @Override public void close() throws Exception {
            input.interrupt(); command(PawNative.INTERRUPT, 0); thread.join(3000);
            assertFalse("Decode thread leaked", thread.isAlive()); input.close();
        }
    }
}
