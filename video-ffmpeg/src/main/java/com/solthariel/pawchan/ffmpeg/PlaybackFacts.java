package com.solthariel.pawchan.ffmpeg;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
/** Immutable metadata publication plus bounded native status snapshots. */
public final class PlaybackFacts {
    private final PlaybackHandle handle;
    private volatile long duration;
    private volatile Map<String, String> metadata = Collections.emptyMap();
    volatile int width, height;
    PlaybackFacts(PlaybackHandle handle) { this.handle = handle; }
    void opened() {
        long[] info = handle.status(); width = (int) info[2]; height = (int) info[3]; duration = info[1];
        String[] entries = handle.metadata();
        Map<String, String> values = new LinkedHashMap<>();
        for (int i = 0; i + 1 < entries.length; i += 2) values.put(entries[i], entries[i + 1]);
        metadata = Collections.unmodifiableMap(values);
    }
    public long getPosition() { return handle.status()[0]; }
    public long getDuration() {
        long observed = handle.status()[1];
        if (observed > 0) duration = observed;
        return duration;
    }
    public Map<String, String> getMetadata() { return metadata; }
}
