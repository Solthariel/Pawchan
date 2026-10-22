package com.solthariel.pawchan.ffmpeg;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicLong;
import okhttp3.Call;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;

/** Two bounded workers: one sequential GET and one cancellable tail/range GET.
 * Native code sees only published, actually written ranges of a session-owned file.
 * Cookies, redirects and HTTPS policy remain with the application's OkHttp client.
 */
public final class ProgressiveSource implements PawPlayback.RangeCallback, AutoCloseable {
    public interface Listener {
        void onFile(File file);
        void onError(IOException error);
    }
    private static final long MAX_BYTES = 1024L * 1024 * 1024;
    private final OkHttpClient client;
    private final File directory;
    private final String url;
    private final Listener listener;
    private final ExecutorService sequential = Executors.newSingleThreadExecutor();
    private final ExecutorService ranges = new java.util.concurrent.ThreadPoolExecutor(
            1, 1, 0, java.util.concurrent.TimeUnit.MILLISECONDS,
            new java.util.concurrent.ArrayBlockingQueue<>(1),
            new java.util.concurrent.ThreadPoolExecutor.DiscardOldestPolicy());
    private final AtomicLong rangeGeneration = new AtomicLong();
    private volatile Call primaryCall;
    private volatile Call rangeCall;
    private volatile PawPlayback player;
    private volatile File file;
    private volatile long progress;
    private volatile long total = -1;
    private volatile String validator;
    private volatile String lastModified;
    private volatile boolean closed;
    private volatile boolean complete;
    private volatile boolean rangesAllowed = true;

    public ProgressiveSource(OkHttpClient client, File directory, String url, Listener listener) {
        this.client = client;
        this.directory = directory;
        this.url = url;
        this.listener = listener;
    }
    public void start() { sequential.execute(this::download); }
    public File getFile() { return file; }
    public long getProgress() { return progress; }
    public boolean isComplete() { return complete; }
    public void bind(PawPlayback player) { this.player = player; }
    @Override public void onReadReady() { publishProgress(); }

    private void download() {
        try {
            File next = File.createTempFile("neko-ffmpeg-", ".media", directory);
            file = next;
            Call call = client.newCall(request().get().build());
            primaryCall = call;
            if (closed) { call.cancel(); return; }
            try (Response response = call.execute()) {
                if (response.code() != 200) throw new IOException("Video HTTP " + response.code());
                if (!"identity".equalsIgnoreCase(response.header("Content-Encoding", "identity")))
                    throw new IOException("Video server ignored identity encoding");
                ResponseBody body = response.body();
                if (body == null) throw new IOException("Empty response body");
                total = body.contentLength();
                if (total > MAX_BYTES) throw new IOException("Video exceeds 1 GiB safety limit");
                String etag = response.header("ETag");
                validator = etag != null && !etag.startsWith("W/") ? etag : null;
                lastModified = response.header("Last-Modified");
                if (!closed) listener.onFile(next);
                copy(body.byteStream(), next, 0, total, false, 0);
                if (progress == 0 || (total >= 0 && progress != total)) throw new IOException("Incomplete video GET");
                total = progress;
                complete = true;
                cancelRange();
                publishProgress();
            }
        } catch (IOException error) {
            if (!closed) listener.onError(error);
        } finally {
            primaryCall = null;
        }
    }
    private Request.Builder request() {
        // Encoded HTTP bodies cannot be addressed using file byte offsets.
        return new Request.Builder().url(url).header("Accept-Encoding", "identity");
    }
    private void publishProgress() {
        PawPlayback active = player;
        if (active != null && !closed) active.input.publish(progress, total);
    }
    @Override public void requestPartFromPosition(long start) {
        cancelRange();
        if (closed || complete || !rangesAllowed || start <= progress || total < 0 || start >= total) {
            publishProgress();
            return;
        }
        // Without an entity validator concurrent requests could merge different files.
        if (validator == null && lastModified == null) { rangesAllowed = false; publishProgress(); return; }
        long token = rangeGeneration.get();
        ranges.execute(() -> downloadRange(start, token));
    }
    private void downloadRange(long start, long token) {
        if (closed || token != rangeGeneration.get()) return;
        Call call = client.newCall(request().header("Range", "bytes=" + start + "-")
                .header("If-Range", validator != null ? validator : lastModified).build());
        rangeCall = call;
        if (closed || token != rangeGeneration.get()) { call.cancel(); return; }
        try (Response response = call.execute()) {
            if (response.code() == 200 || response.code() == 416) {
                rangesAllowed = false;
                publishProgress();
                return;
            }
            if (response.code() != 206) throw new IOException("Range HTTP " + response.code());
            ResponseRange range = ResponseRange.parse(response.header("Content-Range"), start, total);
            String currentValidator = validator != null ? response.header("ETag") : response.header("Last-Modified");
            String expectedValidator = validator != null ? validator : lastModified;
            if (currentValidator != null && !currentValidator.equals(expectedValidator)) throw new IOException("Video changed");
            ResponseBody body = response.body();
            if (body == null) throw new IOException("Empty range body");
            if (body.contentLength() >= 0 && body.contentLength() != range.endExclusive - start)
                throw new IOException("Range body length mismatch");
            copy(body.byteStream(), file, start, range.endExclusive, true, token);
        } catch (IOException error) {
            // A failed optional range never discards the sequential download.
            if (!closed && token == rangeGeneration.get()) { rangesAllowed = false; publishProgress(); }
        } finally {
            if (rangeCall == call) rangeCall = null;
        }
    }
    private void copy(InputStream input, File target, long start, long expectedEnd, boolean partial, long token)
            throws IOException {
        try (InputStream stream = input; RandomAccessFile output = new RandomAccessFile(target, "rw")) {
            output.seek(start);
            byte[] buffer = new byte[64 * 1024];
            long position = start;
            int count;
            while ((count = stream.read(buffer)) != -1) {
                if (closed || (partial && token != rangeGeneration.get())) return;
                if (count == 0) continue;
                if (position > MAX_BYTES - count || (expectedEnd >= 0 && position + count > expectedEnd))
                    throw new IOException("Video response exceeds declared limit");
                output.write(buffer, 0, count);
                position += count;
                if (partial) {
                    PawPlayback active = player;
                    if (active != null && !closed) active.input.publishTail(start, position);
                } else {
                    progress = position;
                    publishProgress();
                }
            }
            if (!closed && expectedEnd >= 0 && position != expectedEnd) throw new IOException("Truncated response body");
        }
    }
    private void cancelRange() {
        rangeGeneration.incrementAndGet();
        Call call = rangeCall;
        if (call != null) call.cancel();
    }
    @Override public void close() {
        closed = true;
        player = null;
        Call call = primaryCall;
        if (call != null) call.cancel();
        cancelRange();
        sequential.shutdownNow();
        ranges.shutdownNow();
    }
    /** Call only after native reader destruction and writer worker termination. */
    public void deleteFile() throws InterruptedException {
        boolean stopped = sequential.awaitTermination(30, java.util.concurrent.TimeUnit.SECONDS)
                && ranges.awaitTermination(30, java.util.concurrent.TimeUnit.SECONDS);
        if (!stopped) return; // Never unlink a live writer's file.
        File target = file;
        if (target != null && !target.delete() && target.exists()) target.deleteOnExit();
    }
}
