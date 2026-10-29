package com.solthariel.pawchan.ffmpeg;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;

/** A single native reader; range publication is synchronized, disk reads stay on the decode thread. */
final class PublishedInput implements AutoCloseable {
    private final TimingProbe timing;
    private final Runnable waiting;
    private final Runnable resumed;
    private RandomAccessFile file;
    private PawPlayback.RangeCallback callback;
    private long prefix, total = -1, tailStart = -1, tailEnd = -1;
    private volatile boolean interrupted;
    PublishedInput(Runnable waiting, Runnable resumed) { this(waiting, resumed, TimingProbe.disabled()); }
    PublishedInput(Runnable waiting, Runnable resumed, TimingProbe timing) {
        this.waiting = waiting; this.resumed = resumed; this.timing = timing;
    }
    boolean timingEnabled() { return timing.enabled(); }
    void timingStage(int stage, long micros) { timing.nativeStage(stage, micros); }
    void open(File target, PawPlayback.RangeCallback callback) throws IOException {
        file = new RandomAccessFile(target, "r");
        this.callback = callback;
        if (callback == null) publish(file.length(), file.length());
        else callback.onReadReady();
    }
    synchronized void publish(long prefix, long total) {
        this.prefix = Math.max(this.prefix, prefix);
        this.total = total;
        notifyAll();
    }
    synchronized void publishTail(long start, long end) {
        tailStart = start; tailEnd = end; notifyAll();
    }
    synchronized long length() { return total; }
    synchronized boolean complete() { return total > 0 && prefix >= total; }
    // JNI: -1 means real EOF, -2 cancellation/error. Unpublished bytes are never returned as zeroes.
    int read(long position, byte[] target, int maximum) {
        boolean busy = false;
        boolean requested = false;
        long waitStarted = 0;
        try {
            while (!interrupted) {
                long available;
                synchronized (this) {
                    available = PublishedRanges.available(position, prefix, tailStart, tailEnd, total);
                    if (available < 0) return (int) available;
                    if (available > 0) {
                        file.seek(position);
                        return file.read(target, 0, (int) Math.min(available, Math.min(target.length, maximum)));
                    }
                }
                if (!busy) {
                    busy = true;
                    waitStarted = timing.now();
                    timing.mark(TimingProbe.Event.READ_WAIT_START, position);
                    waiting.run();
                }
                if (!requested && callback != null) {
                    requested = true;
                    callback.requestPartFromPosition(position);
                }
                synchronized (this) { if (!interrupted) wait(50); }
            }
        } catch (IOException error) {
            return -2;
        } catch (InterruptedException cancelled) {
            Thread.currentThread().interrupt();
        } finally {
            if (busy) {
                timing.mark(TimingProbe.Event.READ_WAIT_END, (timing.now() - waitStarted) / 1000);
                resumed.run();
            }
        }
        return -2;
    }
    synchronized void interrupt() { interrupted = true; notifyAll(); }
    boolean isInterrupted() { return interrupted; }
    @Override public void close() throws IOException { if (file != null) file.close(); }
}
