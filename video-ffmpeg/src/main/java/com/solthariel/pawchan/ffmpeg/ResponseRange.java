package com.solthariel.pawchan.ffmpeg;

import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Strict byte-range validation; a 200 response is never written at a nonzero offset. */
public final class ResponseRange {
    private static final Pattern PATTERN = Pattern.compile("bytes ([0-9]+)-([0-9]+)/([0-9]+)");
    public final long start;
    public final long endExclusive;
    public final long total;
    private ResponseRange(long start, long endExclusive, long total) {
        this.start = start;
        this.endExclusive = endExclusive;
        this.total = total;
    }
    public static ResponseRange parse(String header, long requested, long knownTotal) throws IOException {
        Matcher matcher = PATTERN.matcher(header != null ? header : "");
        if (!matcher.matches()) throw new IOException("Invalid Content-Range");
        try {
            long start = Long.parseLong(matcher.group(1));
            long end = Long.parseLong(matcher.group(2));
            long total = Long.parseLong(matcher.group(3));
            if (start != requested || end < start || end >= total || end == Long.MAX_VALUE
                    || (knownTotal >= 0 && total != knownTotal)) throw new IOException("Mismatched Content-Range");
            return new ResponseRange(start, end + 1, total);
        } catch (NumberFormatException exception) {
            throw new IOException("Content-Range overflow", exception);
        }
    }
}
