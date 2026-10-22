package com.solthariel.pawchan.ffmpeg;

import android.content.Context;
import android.graphics.Matrix;
import android.graphics.SurfaceTexture;
import android.view.Surface;
import android.view.TextureView;

/** Each view owns its texture. Decoder state and the retained RGBA frame do not belong to a view. */
final class PlaybackTexture extends TextureView implements TextureView.SurfaceTextureListener {
    private final PlaybackVideo owner;
    private boolean detached;
    private int videoWidth, videoHeight;
    /** A standalone/editor texture has no playback session or native surface to publish. */
    public PlaybackTexture(Context context) { this(context, null); }
    PlaybackTexture(Context context, PlaybackVideo owner) {
        super(context); this.owner = owner; setSurfaceTextureListener(this); setOpaque(false);
    }
    void dimensions(int width, int height) { videoWidth = width; videoHeight = height; fit(); }
    private void fit() {
        if (videoWidth <= 0 || videoHeight <= 0 || getWidth() <= 0 || getHeight() <= 0) return;
        float factor = Math.min(getWidth() / (float) videoWidth, getHeight() / (float) videoHeight);
        Matrix transform = new Matrix();
        transform.setScale(videoWidth * factor / getWidth(), videoHeight * factor / getHeight(),
                getWidth() / 2f, getHeight() / 2f);
        setTransform(transform);
    }
    @Override public void onSurfaceTextureAvailable(SurfaceTexture texture, int width, int height) {
        if (detached || owner == null) return;
        Surface surface = new Surface(texture);
        try { owner.surface(surface); } finally { surface.release(); }
        fit();
    }
    @Override public void onSurfaceTextureSizeChanged(SurfaceTexture texture, int width, int height) { fit(); }
    @Override public boolean onSurfaceTextureDestroyed(SurfaceTexture texture) {
        if (!detached && owner != null) owner.surface(null);
        return true;
    }
    @Override public void onSurfaceTextureUpdated(SurfaceTexture texture) { if (!detached && owner != null) owner.frameVisible(); }
    void detach() { detached = true; if (owner != null) owner.surface(null); }
}
