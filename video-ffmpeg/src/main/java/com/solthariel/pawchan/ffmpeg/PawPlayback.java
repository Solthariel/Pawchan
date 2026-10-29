package com.solthariel.pawchan.ffmpeg;
import android.os.Handler;
import android.os.Looper;
import java.io.File;
import java.io.IOException;
import java.util.concurrent.CountDownLatch;
import java.util.function.Consumer;

/** Owns one decode thread, byte source and PCM sink. JNI never receives a URL or authentication. */
public final class PawPlayback implements AutoCloseable {
    public interface Listener {
        void onComplete(PawPlayback player);
        void onBusyStateChange(PawPlayback player, boolean busy);
        void onDimensionChange(PawPlayback player);
        void onVideoOutputReset(PawPlayback player);
        void onError(PawPlayback player);
        default void onIncomplete(PawPlayback player) { onError(player); }
    }
    public interface RangeCallback {
        void onReadReady();
        void requestPartFromPosition(long position);
    }
    private final Handler main = new Handler(Looper.getMainLooper());
    private final PlaybackHandle handle = new PlaybackHandle();
    private final PcmOutput audio = new PcmOutput();
    final PublishedInput input;
    public final PlaybackControls controls = new PlaybackControls(handle, audio);
    public final PlaybackFacts facts = new PlaybackFacts(handle);
    public final PlaybackVideo video = new PlaybackVideo(handle, facts, () -> dispatch(l -> l.onVideoOutputReset(this)));
    private volatile Listener listener;
    private Thread decoder;
    private boolean closed;
    private volatile IOException openingError;
    public PawPlayback(Listener listener) { this(listener, TimingProbe.disabled()); }
    public PawPlayback(Listener listener, TimingProbe timing) {
        this.listener = listener;
        input = new PublishedInput(() -> busy(true), () -> busy(false), timing);
    }
    public static void loadLibraries() { Libraries.ensure(); }
    public void init(File file, RangeCallback callback) throws IOException {
        input.open(file, callback);
        CountDownLatch ready = new CountDownLatch(1);
        synchronized (this) {
            if (closed || input.isInterrupted()) throw new IOException("Playback cancelled");
            decoder = new Thread(() -> decode(ready), "paw-ffmpeg-decode"); decoder.start();
        }
        try { ready.await(); }
        catch (InterruptedException cancelled) {
            interruptRead(); Thread.currentThread().interrupt(); throw new IOException(cancelled);
        }
        if (openingError != null) throw openingError;
        if (input.isInterrupted()) throw new IOException("Playback cancelled");
        dispatch(l -> l.onDimensionChange(this));
    }
    private void decode(CountDownLatch ready) {
        try {
            handle.allocate();
            if (!handle.open(input, audio, controls.software)) throw new IOException("Unsupported or damaged media");
            facts.opened(); controls.apply(); ready.countDown();
            while (!input.isInterrupted()) {
                int result = handle.pump();
                if (result < 0) throw new IOException("Decode failed");
                if (result == 1 || result == 2) {
                    long completedRevision = handle.status()[5];
                    dispatch(l -> {
                        if (handle.status()[4] != completedRevision) return;
                        if (result == 2) l.onIncomplete(this); else l.onComplete(this);
                    });
                }
            }
        } catch (IOException | RuntimeException failure) {
            openingError = failure instanceof IOException ? (IOException) failure : new IOException(failure);
            if (!input.isInterrupted()) dispatch(l -> l.onError(this));
        } finally { handle.dispose(); ready.countDown(); }
    }
    public void setListener(Listener next) { listener = next; }
    private void dispatch(Consumer<Listener> action) {
        main.post(() -> { Listener active = listener; if (active != null) action.accept(active); });
    }
    private void busy(boolean value) {
        handle.command(5, value ? 1 : 0); dispatch(l -> l.onBusyStateChange(this, value));
    }
    public void interruptRead() {
        input.interrupt(); audio.cancel(); handle.command(PawNative.INTERRUPT, 0);
    }
    @Override public void close() throws IOException {
        interruptRead(); Thread active;
        synchronized (this) { closed = true; active = decoder; }
        if (active != null) {
            try { active.join(); }
            catch (InterruptedException cancelled) { Thread.currentThread().interrupt(); throw new IOException(cancelled); }
        }
        audio.close(); input.close();
    }
    private static final class Libraries {
        static { System.loadLibrary("pawplayback"); }
        private Libraries() {}
        static void ensure() { /* JVM initialization serializes immutable library loading. */ }
    }
}
