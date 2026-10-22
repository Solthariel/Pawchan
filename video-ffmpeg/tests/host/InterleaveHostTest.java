package com.solthariel.pawchan.ffmpeg;
import static org.junit.Assert.*;
import java.io.File;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;
import org.junit.Test;
/** Real demux/decode/JNI and finite PCM sink; no Android/MediaCodec performance claim. */
public final class InterleaveHostTest {
    static { System.load(System.getProperty("paw.hostLibrary")); }
    @Test public void originalProblemFilesDoNotBlockVideoBehindPcmChunks() throws Exception {
        String[] files = System.getProperty("paw.problemFiles").split(File.pathSeparator);
        for (String name : files) {
            try (Session session = new Session(new File(name))) {
                session.command(PawNative.PLAY, 1);
                long start = System.nanoTime(), lastChange = start, worst = 0, previous = -1;
                while (!session.ended.isDone() && System.nanoTime() - start < TimeUnit.SECONDS.toNanos(35)) {
                    long position = session.status()[0], now = System.nanoTime();
                    if (position != previous) {
                        if (previous >= 100 && position < session.duration - 400) worst = Math.max(worst, now - lastChange);
                        previous = position; lastChange = now;
                    }
                    Thread.sleep(5);
                }
                assertEquals(1, (int) session.ended.get(1, TimeUnit.SECONDS));
                double gapMs = worst / 1000000.0;
                System.out.println(new File(name).getName() + ": worst video progress gap=" + gapMs + " ms");
                assertTrue(name + ": recurring stall " + gapMs + " ms", gapMs < 300);
                assertTrue("No audio decoded", session.audio.bytes > 0);
            }
        }
    }
    @Test public void queuedPacketsAreDiscardedOnPausedAndLiveSeek() throws Exception {
        File file = new File(System.getProperty("paw.problemFiles").split(File.pathSeparator)[0]);
        try (Session session = new Session(file)) {
            session.command(PawNative.PLAY, 1); awaitPosition(session, 150);
            session.audio.play(false); session.command(PawNative.PLAY, 0);
            Thread.sleep(80); long frozen = session.status()[0]; Thread.sleep(120);
            assertEquals(frozen, session.status()[0]);
            session.command(PawNative.SEEK, 2500); awaitPosition(session, 2500);
            long preview = session.status()[0]; Thread.sleep(120);
            assertEquals("Paused preview advanced", preview, session.status()[0]);
            session.audio.play(true); session.command(PawNative.PLAY, 1); awaitPosition(session, 2800);
            session.command(PawNative.SEEK, 1000);
            long until = System.nanoTime() + TimeUnit.SECONDS.toNanos(3);
            while (session.status()[0] >= 2500 && System.nanoTime() < until) Thread.sleep(5);
            assertTrue("Old prefetched frame survived seek", session.status()[0] < 2500);
            awaitPosition(session, 1400);
        }
    }
    private static void awaitPosition(Session session, long position) throws Exception {
        long until = System.nanoTime() + TimeUnit.SECONDS.toNanos(3);
        while (session.status()[0] < position && System.nanoTime() < until && !session.ended.isDone()) Thread.sleep(5);
        assertTrue("Target not reached: " + position, session.status()[0] >= position);
    }
    private static final class Session implements AutoCloseable {
        final PublishedInput input = new PublishedInput(() -> {}, () -> {});
        final PcmOutput audio = new PcmOutput();
        final AtomicLong handle = new AtomicLong();
        final CompletableFuture<Void> opened = new CompletableFuture<>();
        final CompletableFuture<Integer> ended = new CompletableFuture<>();
        final Thread thread;
        final long duration;
        Session(File file) throws Exception {
            input.open(file, null); audio.enableRealtime();
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
                } catch (Throwable error) { opened.completeExceptionally(error); ended.completeExceptionally(error); }
                finally { synchronized (handle) { handle.set(0); PawNative.destroy(id); } }
            }, "paw-interleave-host");
            thread.start();
            try { opened.get(5, TimeUnit.SECONDS); duration = status()[1]; }
            catch (Exception error) { close(); throw error; }
        }
        void command(int type, long value) {
            synchronized (handle) { if (handle.get() != 0) PawNative.command(handle.get(), type, value); }
        }
        long[] status() {
            synchronized (handle) { return handle.get() == 0 ? new long[6] : PawNative.status(handle.get()); }
        }
        @Override public void close() throws Exception {
            input.interrupt(); audio.cancel(); command(PawNative.INTERRUPT, 0); thread.join(3000);
            assertFalse("Decode thread leaked", thread.isAlive()); input.close();
        }
    }
}
