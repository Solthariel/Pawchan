package com.solthariel.pawchan.ffmpeg;
import static org.junit.Assert.*;
import java.io.File;
import java.nio.file.Files;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.Test;
public final class PublishedInputTest {
    @Test public void localReadHonorsRequestedCapacity() throws Exception {
        File file = File.createTempFile("paw-input", ".bin"); Files.write(file.toPath(), new byte[]{1,2,3,4});
        try (PublishedInput input = new PublishedInput(() -> {}, () -> {})) {
            input.open(file, null); byte[] bytes = new byte[8];
            assertEquals(2, input.read(0, bytes, 2)); assertEquals(2, bytes[1]);
            assertEquals(0, bytes[2]); assertEquals(-1, input.read(4, bytes, 8));
        } finally { file.delete(); }
    }
    @Test public void cancellationWakesUnpublishedRead() throws Exception {
        File file = File.createTempFile("paw-input", ".bin");
        CountDownLatch waiting = new CountDownLatch(1);
        ExecutorService thread = Executors.newSingleThreadExecutor();
        try (PublishedInput input = new PublishedInput(waiting::countDown, () -> {})) {
            input.open(file, callback());
            Future<Integer> result = thread.submit(() -> input.read(0, new byte[4], 4));
            assertTrue(waiting.await(1, TimeUnit.SECONDS)); input.interrupt();
            assertEquals(-2, (int) result.get(1, TimeUnit.SECONDS));
        } finally { thread.shutdownNow(); file.delete(); }
    }
    @Test public void publishedBytesResumeWithoutFalseEof() throws Exception {
        File file = File.createTempFile("paw-input", ".bin"); Files.write(file.toPath(), new byte[]{7,8,9});
        CountDownLatch waiting = new CountDownLatch(1); AtomicInteger resumed = new AtomicInteger();
        ExecutorService thread = Executors.newSingleThreadExecutor();
        try (PublishedInput input = new PublishedInput(waiting::countDown, resumed::incrementAndGet)) {
            input.open(file, callback()); byte[] output = new byte[4];
            Future<Integer> result = thread.submit(() -> input.read(0, output, 4));
            assertTrue(waiting.await(1, TimeUnit.SECONDS)); assertFalse(result.isDone());
            input.publish(3, 3); assertEquals(3, (int) result.get(1, TimeUnit.SECONDS));
            assertEquals(1, resumed.get()); assertEquals(7, output[0]);
        } finally { thread.shutdownNow(); file.delete(); }
    }
    @Test public void physicalFileAndTailDoNotImplyCompletePublication() throws Exception {
        File file = File.createTempFile("paw-prefix", ".bin");
        Files.write(file.toPath(), new byte[100]);
        try (PublishedInput input = new PublishedInput(() -> {}, () -> {})) {
            input.open(file, callback());
            assertFalse(input.complete());
            input.publish(20, 100);
            input.publishTail(80, 100);
            assertFalse(input.complete());
            input.publish(100, 100);
            assertTrue(input.complete());
        } finally { file.delete(); }
    }
    @Test public void localNonemptyFileIsCompleteImmediately() throws Exception {
        File file = File.createTempFile("paw-complete", ".bin");
        Files.write(file.toPath(), new byte[]{1});
        try (PublishedInput input = new PublishedInput(() -> {}, () -> {})) {
            input.open(file, null);
            assertTrue(input.complete());
        } finally { file.delete(); }
    }
    private static PawPlayback.RangeCallback callback() {
        return new PawPlayback.RangeCallback() { public void onReadReady() {} public void requestPartFromPosition(long offset) {} };
    }
}
