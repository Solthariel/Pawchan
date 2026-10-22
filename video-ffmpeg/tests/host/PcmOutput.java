package com.solthariel.pawchan.ffmpeg;
/** HOST TEST ONLY: optional finite realtime queue, not Android AudioTrack or a driver. */
final class PcmOutput implements AutoCloseable {
    volatile long bytes;
    private boolean realtime, playing = true;
    private long queued, tick;
    private static final long CAPACITY = 76800; // Production PCM capacity, 400 ms at 1x.
    synchronized void enableRealtime() { realtime = true; tick = System.nanoTime(); }
    private void advance() {
        long now = System.nanoTime();
        if (playing) queued = Math.max(0, queued - (now - tick) * 192000 / 1000000000L);
        tick = now;
    }
    void configure() {}
    synchronized int write(byte[] data, int offset) {
        if (!realtime) { bytes += data.length - offset; return data.length - offset; }
        advance();
        int count = playing ? (int) Math.min(data.length - offset, CAPACITY - queued) & ~3 : 0;
        queued += count; bytes += count;
        return count;
    }
    synchronized long remaining() { if (!realtime) return 0; advance(); return queued / 4; }
    synchronized void flush() { queued = 0; tick = System.nanoTime(); }
    synchronized void play(boolean value) { if (realtime) advance(); playing = value; }
    int sound(int volume, boolean muted, int speed) { return speed; }
    String diagnostics() { return "host PCM"; }
    synchronized void cancel() { playing = false; queued = 0; }
    @Override public void close() {}
}
