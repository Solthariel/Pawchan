package com.solthariel.pawchan.ffmpeg;
import static org.junit.Assert.*;
import org.junit.Test;
public final class PublishedRangesTest {
    @Test public void prefixIsBounded() { assertEquals(12, PublishedRanges.available(8, 20, -1, -1, 100)); }
    @Test public void holesAreNotData() { assertEquals(0, PublishedRanges.available(30, 20, 80, 100, 100)); }
    @Test public void tailPreservesCoordinates() { assertEquals(13, PublishedRanges.available(87, 20, 80, 100, 100)); }
    @Test public void unknownLengthDoesNotMeanEof() { assertEquals(0, PublishedRanges.available(50, 20, -1, -1, -1)); }
    @Test public void eofRequiresKnownLength() { assertEquals(-1, PublishedRanges.available(100, 100, -1, -1, 100)); }
    @Test public void invalidOffsetIsRejected() { assertEquals(-2, PublishedRanges.available(-1, 20, -1, -1, 100)); }
    @Test public void publicationCannotCrossEntityLength() { assertEquals(5, PublishedRanges.available(15, 100, -1, -1, 20)); }
}
