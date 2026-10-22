package com.solthariel.pawchan.ffmpeg;

import android.media.AudioAttributes;
import android.media.AudioFormat;
import android.media.AudioTrack;
import android.media.PlaybackParams;
import android.os.Build;
import android.util.Log;

/** Android time stretch; buffer capacity covers 2x while the start threshold stays small. */
final class PcmOutput implements AutoCloseable {
    private AudioTrack track;
    private boolean closed, playing;
    private float volume = 1f;
    private int speed = 1000;
    private long writtenBytes;
    synchronized void configure() {
        if (closed || track != null) return;
        int minimum = AudioTrack.getMinBufferSize(48000, AudioFormat.CHANNEL_OUT_STEREO,
                AudioFormat.ENCODING_PCM_16BIT);
        if (minimum <= 0) throw new IllegalStateException("PCM output unavailable");
        AudioTrack next = new AudioTrack.Builder()
                .setAudioAttributes(new AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_MEDIA)
                        .setContentType(AudioAttributes.CONTENT_TYPE_MOVIE).build())
                .setAudioFormat(new AudioFormat.Builder().setSampleRate(48000)
                        .setChannelMask(AudioFormat.CHANNEL_OUT_STEREO)
                        .setEncoding(AudioFormat.ENCODING_PCM_16BIT).build())
                .setBufferSizeInBytes(Math.max(minimum * 4, 76800))
                .setTransferMode(AudioTrack.MODE_STREAM).build();
        try {
            next.setVolume(volume);
            next.setPlaybackParams(new PlaybackParams().setSpeed(speed / 1000f).setPitch(1f));
            // Capacity and startup threshold are different. A full 400 ms buffer must not
            // be required to start a short clip or restart after a seek/underrun.
            if (Build.VERSION.SDK_INT >= 31) next.setStartThresholdInFrames(480);
            if (playing) next.play(); else next.pause();
            track = next;
        } catch (RuntimeException failure) {
            next.release();
            throw failure; // Decode owner reports a controlled open/decode error, not a UI crash.
        }
    }
    synchronized void play(boolean value) {
        playing = value;
        if (track != null) { if (value) track.play(); else track.pause(); }
    }
    synchronized int sound(int percent, boolean muted, int milli) {
        volume = muted ? 0f : percent / 100f;
        if (track != null) {
            track.setVolume(volume);
            if (milli != speed) {
                try {
                    track.setPlaybackParams(new PlaybackParams().setSpeed(milli / 1000f).setPitch(1f));
                } catch (IllegalArgumentException rejected) {
                    Log.w("PawchanPcm", "AudioTrack rejected speed " + milli + "; keeping " + speed, rejected);
                    return speed;
                }
                // setPlaybackParams may start a paused track; preserve user intent.
                if (playing) track.play(); else track.pause();
            }
        }
        speed = milli;
        return speed;
    }
    // One nonblocking call: the native pump owns retries, pause/seek and surface work.
    synchronized int write(byte[] data, int offset) {
        if (closed) return -1;
        if (!playing || track == null) return 0;
        int count = track.write(data, offset, data.length - offset, AudioTrack.WRITE_NON_BLOCKING);
        if (count > 0) writtenBytes += count;
        return count;
    }
    synchronized long remaining() {
        return closed || track == null ? 0 : Math.max(0, writtenBytes / 4 - (track.getPlaybackHeadPosition() & 0xffffffffL));
    }
    synchronized void flush() {
        if (track == null) return;
        track.pause(); track.flush(); writtenBytes = 0;
        if (playing) track.play();
    }
    synchronized String diagnostics() {
        return "requested=" + playing + " track=" + (track == null ? -1 : track.getPlayState())
                + " queued=" + remaining() + " speed=" + speed;
    }
    synchronized void cancel() { closed = true; if (track != null) track.pause(); }
    @Override public synchronized void close() {
        closed = true;
        if (track != null) { track.release(); track = null; }
    }
}
