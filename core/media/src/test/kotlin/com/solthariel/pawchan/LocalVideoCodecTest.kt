package com.solthariel.pawchan

import com.solthariel.pawchan.media.metadata.basicMediaSummary
import com.solthariel.pawchan.media.metadata.localVideoCodec
import org.junit.Assert.assertEquals
import org.junit.Test

class LocalVideoCodecTest {
    @Test fun labelsMatchViewerCodecNames() {
        assertEquals("H.264", localVideoCodec("video/avc"))
        assertEquals("H.265", localVideoCodec("video/hevc"))
        assertEquals("VP8", localVideoCodec("video/x-vnd.on2.vp8"))
        assertEquals("VP9", localVideoCodec("video/x-vnd.on2.vp9"))
        assertEquals("AV1", localVideoCodec("video/av01"))
        assertEquals("MPEG-4", localVideoCodec("video/mp4v-es"))
        assertEquals("H.263", localVideoCodec("video/3gpp"))
    }

    @Test fun unknownCodecKeepsItsMimeSubtypeAndCaseDoesNotMatter() {
        assertEquals("H.264", localVideoCodec("VIDEO/AVC"))
        assertEquals("CUSTOM", localVideoCodec("video/custom"))
    }

    @Test fun fallbackSummaryUsesTheSameFieldOrderAndSeparator() {
        assertEquals("JPEG · 1320×925 · 107 КБ", basicMediaSummary("JPEG", "1320×925", 107_520))
        assertEquals("MP4 · 1.0 КБ", basicMediaSummary("MP4", null, 1024))
    }

    @Test fun fallbackSummaryPreservesSharedSizePrecisionAtUnitBoundaries() {
        assertEquals("MP4 · 1.5 КБ", basicMediaSummary("MP4", null, 1_536))
        assertEquals("MP4 · 10 КБ", basicMediaSummary("MP4", null, 10_240))
        assertEquals("MP4 · 1.0 МБ", basicMediaSummary("MP4", null, 1_048_576))
    }
}
