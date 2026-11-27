package com.solthariel.pawchan.ffmpeg;

import com.sun.net.httpserver.HttpServer;
import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import io.github.yuroyami.libmpvkt.stream.MpvStream;
import okhttp3.OkHttpClient;
import org.junit.Test;
import static org.junit.Assert.*;

/** Host-only integration tests: actual OkHttp and HTTP server, no mocked native playback. */
public class ProgressiveSourceHostTest {
    @Test public void bundledThreeSecondClipReadsToEofElevenTimesWithOneRealGet() throws Exception {
        Path asset = Path.of("core/media/src/androidTest/assets/video/h264_aac.mp4");
        if (!Files.isRegularFile(asset)) asset = Path.of("..").resolve(asset);
        byte[] bytes = Files.readAllBytes(asset);
        AtomicInteger gets = new AtomicInteger();
        HttpServer server = HttpServer.create(new java.net.InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/clip.mp4", exchange -> {
            try {
                if (!"GET".equals(exchange.getRequestMethod())) {
                    exchange.sendResponseHeaders(405, -1);
                    return;
                }
                gets.incrementAndGet();
                exchange.sendResponseHeaders(200, bytes.length);
                exchange.getResponseBody().write(bytes);
            } finally { exchange.close(); }
        });
        server.start();
        File dir = Files.createTempDirectory("paw-clip-loop").toFile();
        AtomicReference<IOException> failure = new AtomicReference<>();
        ProgressiveSource source = new ProgressiveSource(new OkHttpClient(), dir,
                "http://127.0.0.1:" + server.getAddress().getPort() + "/clip.mp4", new ProgressiveSource.Listener() {
                    public void onFile(File file) {}
                    public void onError(IOException e) { failure.set(e); }
                });
        try {
            source.start();
            await(() -> source.isComplete() || failure.get() != null);
            assertNull(failure.get());
            try (MpvProgressiveProvider provider = new MpvProgressiveProvider(source.getFile(), source, () -> {}, () -> {})) {
                for (int pass = 0; pass <= 10; pass++) {
                    MpvStream stream = java.util.Objects.requireNonNull(provider.open("pawvideo://video"));
                    try {
                        ByteBuffer result = ByteBuffer.allocate(bytes.length);
                        while (result.hasRemaining()) assertTrue(stream.read(result) > 0);
                        assertArrayEquals(bytes, result.array());
                        assertEquals(0, stream.read(ByteBuffer.allocate(1)));
                    } finally { stream.close(); }
                }
                assertEquals(1, gets.get());
            }
        } finally {
            source.close(); source.deleteFile(); server.stop(0);
            await(() -> source.getFile() == null || !source.getFile().exists());
            dir.delete();
        }
    }

    @Test public void startsBeforeWholeFileAndPreservesBytes() throws Exception { exerciseRange(206); }
    @Test public void ignoredRangeDoesNotCorruptPartialFile() throws Exception { exerciseRange(200); }
    @Test public void range416KeepsSequentialDownload() throws Exception { exerciseRange(416); }

    private void exerciseRange(int rangeStatus) throws Exception {
        byte[] bytes = new byte[256];
        for (int i = 0; i < bytes.length; i++) bytes[i] = (byte)i;
        CountDownLatch resume = new CountDownLatch(1);
        CountDownLatch rangeDone = new CountDownLatch(1);
        CountDownLatch fileReady = new CountDownLatch(1);
        AtomicReference<IOException> failure = new AtomicReference<>();
        java.util.List<String> events = new java.util.concurrent.CopyOnWriteArrayList<>();
        TimingProbe timing = new TimingProbe(true, System::nanoTime, events::add);
        HttpServer server = HttpServer.create(new java.net.InetSocketAddress("127.0.0.1", 0), 0);
        java.util.concurrent.ExecutorService network = java.util.concurrent.Executors.newFixedThreadPool(2);
        server.setExecutor(network);
        server.createContext("/video", exchange -> {
            try {
                exchange.getResponseHeaders().set("ETag", "\"same-file\"");
                if (exchange.getRequestHeaders().getFirst("Range") != null) {
                    if (rangeStatus == 206) {
                        exchange.getResponseHeaders().set("Content-Range", "bytes 128-255/256");
                        exchange.sendResponseHeaders(206, 128);
                        exchange.getResponseBody().write(Arrays.copyOfRange(bytes, 128, 256));
                    } else if (rangeStatus == 200) {
                        exchange.sendResponseHeaders(200, 256);
                        exchange.getResponseBody().write(bytes);
                    } else {
                        exchange.sendResponseHeaders(416, -1);
                    }
                    rangeDone.countDown();
                } else {
                    exchange.sendResponseHeaders(200, 256);
                    exchange.getResponseBody().write(bytes, 0, 64);
                    exchange.getResponseBody().flush();
                    resume.await(5, TimeUnit.SECONDS);
                    exchange.getResponseBody().write(bytes, 64, 192);
                }
            } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
            finally { exchange.close(); }
        });
        server.start();
        File directory = Files.createTempDirectory("neko-source-test").toFile();
        ProgressiveSource source = new ProgressiveSource(new OkHttpClient(), directory,
                "http://127.0.0.1:" + server.getAddress().getPort() + "/video", new ProgressiveSource.Listener() {
            public void onFile(File file) { fileReady.countDown(); }
            public void onError(IOException e) { failure.set(e); }
        }, timing);
        try {
            source.start();
            assertTrue(fileReady.await(3, TimeUnit.SECONDS));
            await(() -> source.getProgress() >= 64);
            assertFalse(source.isComplete());
            source.requestPartFromPosition(128);
            assertTrue(rangeDone.await(3, TimeUnit.SECONDS));
            // Wait for the client to observe the headers before completing the primary GET,
            // which is allowed to cancel the optional range as soon as the file is complete.
            await(() -> events.stream().anyMatch(line -> line.contains("event=RANGE_HEADERS value=" + rangeStatus)));
            if (rangeStatus != 206) {
                await(() -> events.stream().anyMatch(line -> line.contains("event=RANGE_UNAVAILABLE value=" + rangeStatus)));
                Thread.sleep(100);
                assertEquals("Full response must not be appended at range offset", 64, source.getFile().length());
            }
            resume.countDown();
            await(source::isComplete);
            assertNull(failure.get());
            assertArrayEquals(bytes, Files.readAllBytes(source.getFile().toPath()));
            await(() -> events.stream().anyMatch(line -> line.contains("event=GET_COMPLETE value=256")));
            assertTrue(events.stream().anyMatch(line -> line.contains("event=GET_HEADERS value=200")));
            assertTrue(events.stream().anyMatch(line -> line.contains("event=GET_FIRST_BYTES value=64")));
            assertTrue(events.stream().anyMatch(line -> line.contains("event=RANGE_HEADERS value=" + rangeStatus)));
            if (rangeStatus != 206) {
                assertTrue(events.stream().anyMatch(line -> line.contains("event=RANGE_UNAVAILABLE value=" + rangeStatus)));
            }
            assertTrue(events.stream().noneMatch(line -> line.contains("127.0.0.1") || line.contains("same-file")));
        } finally {
            resume.countDown(); source.close(); source.deleteFile(); server.stop(0); network.shutdownNow();
            directory.delete();
        }
    }
    @Test public void cancellationUnblocksBodyReadAndDeletesFile() throws Exception {
        HttpServer server = HttpServer.create(new java.net.InetSocketAddress("127.0.0.1", 0), 0);
        CountDownLatch release = new CountDownLatch(1);
        server.createContext("/video", exchange -> {
            try {
                exchange.sendResponseHeaders(200, 1024);
                exchange.getResponseBody().write(new byte[64]);
                exchange.getResponseBody().flush();
                release.await(5, TimeUnit.SECONDS);
            } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
            finally { exchange.close(); }
        });
        server.start();
        File directory = Files.createTempDirectory("neko-source-cancel").toFile();
        ProgressiveSource source = new ProgressiveSource(new OkHttpClient(), directory,
                "http://127.0.0.1:" + server.getAddress().getPort() + "/video", new ProgressiveSource.Listener() {
            public void onFile(File file) {}
            public void onError(IOException e) {}
        });
        try {
            source.start(); await(() -> source.getProgress() >= 64);
            File file = source.getFile(); source.close(); source.deleteFile();
            await(() -> !file.exists());
            assertFalse(file.exists());
        } finally { release.countDown(); server.stop(0); directory.delete(); }
    }
    private static void await(java.util.function.BooleanSupplier condition) throws Exception {
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(5);
        while (!condition.getAsBoolean() && System.nanoTime() < deadline) Thread.sleep(10);
        assertTrue("Timed out waiting for source", condition.getAsBoolean());
    }
}
