package com.solthariel.pawchan.ffmpeg;

/** Decides how many verified bytes can be consumed, independently of sparse-file length. */
final class PublishedRanges {
    private PublishedRanges() {}
    static long available(long at, long prefix, long tailStart, long tailEnd, long total) {
        if (at < 0) return -2;
        if (total >= 0 && at >= total) return -1;
        long end = at < prefix ? prefix : at;
        if (at >= tailStart && at < tailEnd) end = Math.max(end, tailEnd);
        if (total >= 0) end = Math.min(end, total);
        return Math.max(0, end - at);
    }
}
