package com.solthariel.pawchan.ffmpeg;

import android.util.Log;
import android.view.View;

/** Bounded Android log integration. Never receives source URLs or cookies. */
final class VideoDiagnostics {
    private VideoDiagnostics() {}
    static boolean isRecording() { return false; }
    static boolean isExtendedRecording() { return false; }
    static void recordUi(String text) { Log.d("NekoFFmpeg", text); }
    static void recordViewGeometry(String phase, View view) {
        Log.d("NekoFFmpeg", phase + " " + view.getWidth() + "x" + view.getHeight());
    }
    static void surfaceAvailable(int id) { Log.d("NekoFFmpeg", "surface_available " + id); }
    static void surfaceDestroyed(int id) { Log.d("NekoFFmpeg", "surface_destroyed " + id); }
    static void surfaceUpdated(int id) { /* Frame-rate callback deliberately does not log. */ }
}
