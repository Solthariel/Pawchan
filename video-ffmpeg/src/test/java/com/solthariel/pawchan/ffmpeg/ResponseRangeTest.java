package com.solthariel.pawchan.ffmpeg;

import java.io.IOException;
import org.junit.Test;
import static org.junit.Assert.assertEquals;

public final class ResponseRangeTest {
    @Test public void acceptsMatchingRange() throws IOException {
        ResponseRange range = ResponseRange.parse("bytes 100-199/200", 100, 200);
        assertEquals(100, range.start);
        assertEquals(200, range.endExclusive);
    }
    @Test(expected = IOException.class) public void rejectsWrongOffset() throws IOException {
        ResponseRange.parse("bytes 0-199/200", 100, 200);
    }
    @Test(expected = IOException.class) public void rejectsChangedEntityLength() throws IOException {
        ResponseRange.parse("bytes 100-199/200", 100, 300);
    }
    @Test(expected = IOException.class) public void rejectsOutOfBounds() throws IOException {
        ResponseRange.parse("bytes 100-200/200", 100, 200);
    }
    @Test(expected = IOException.class) public void rejectsUnknownAndOverflow() throws IOException {
        ResponseRange.parse("bytes 100-999999999999999999999999/200", 100, 200);
    }
}
