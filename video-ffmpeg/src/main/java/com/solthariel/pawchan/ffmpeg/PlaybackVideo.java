package com.solthariel.pawchan.ffmpeg;
import android.content.Context;
import android.graphics.Bitmap;
import android.view.Surface;
import android.view.View;
/** Main-thread texture ownership, independent of file/decoder ownership. */
public final class PlaybackVideo {
    private final PlaybackHandle handle;
    private final PlaybackFacts facts;
    private final Runnable reset;
    private PlaybackTexture texture;
    private Runnable frameCallback;
    PlaybackVideo(PlaybackHandle handle, PlaybackFacts facts, Runnable reset) {
        this.handle = handle; this.facts = facts; this.reset = reset;
    }
    public View getView(Context context) {
        release(); texture = new PlaybackTexture(context, this);
        texture.dimensions(facts.width, facts.height); return texture;
    }
    public void release() { if (texture != null) { texture.detach(); texture = null; } }
    public Bitmap snapshot(int maximum) {
        if (texture == null || !texture.isAvailable()) return null;
        float ratio = Math.min(1f, maximum / (float) Math.max(texture.getWidth(), texture.getHeight()));
        return texture.getBitmap(Math.max(1, (int) (texture.getWidth() * ratio)),
                Math.max(1, (int) (texture.getHeight() * ratio)));
    }
    public void onFrame(Runnable callback) { frameCallback = callback; }
    void surface(Surface surface) { handle.surface(surface); if (surface != null) reset.run(); }
    void frameVisible() { Runnable next = frameCallback; frameCallback = null; if (next != null) next.run(); }
}
