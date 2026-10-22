package com.solthariel.pawchan.ffmpeg;
/** User intent survives loading; native controls never run a decode or wait for a network read. */
public final class PlaybackControls {
    private final PlaybackHandle handle;
    private final PcmOutput audio;
    volatile boolean software;
    private volatile boolean playing, muted;
    private volatile int volume = 100, speed = 1000;
    PlaybackControls(PlaybackHandle handle, PcmOutput audio) { this.handle = handle; this.audio = audio; }
    public void setHardwareEnabled(boolean enabled) { software = !enabled; }
    public synchronized void setPlaying(boolean value) {
        playing = value; audio.play(value); handle.command(PawNative.PLAY, value ? 1 : 0);
    }
    public void setPosition(long position) { handle.command(PawNative.SEEK, Math.max(0, position)); audio.flush(); }
    public synchronized int setPlaybackSpeed(int milli) {
        speed = audio.sound(volume, muted, Math.max(500, Math.min(2000, milli)));
        handle.command(PawNative.SPEED, speed);
        return speed;
    }
    public synchronized void setVolume(int percent) { volume = Math.max(0, Math.min(100, percent)); audio.sound(volume, muted, speed); }
    public synchronized void setMuted(boolean value) { muted = value; audio.sound(volume, muted, speed); }
    public String diagnostics() { return audio.diagnostics(); }
    synchronized void apply() {
        speed = audio.sound(volume, muted, speed); audio.play(playing); handle.command(PawNative.SPEED, speed);
        handle.command(PawNative.PLAY, playing ? 1 : 0);
    }
}
