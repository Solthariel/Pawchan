package com.solthariel.pawchan

import com.solthariel.pawchan.media.metadata.videoMetadata
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class VideoDecoderMetadataTest {
    @Test fun registeredDecoderNameRemainsVisibleForHardwareAndSoftware() {
        listOf("h264_mediacodec", "hevc_mediacodec", "vp8_mediacodec", "vp9_mediacodec", "av1_mediacodec", "h264")
            .forEach { name ->
                val metadata = videoMetadata(mapOf("decoder_name" to name), 0, null)
                val row =
                    metadata.sections
                        .single()
                        .rows
                        .single()
                assertEquals("Видео", metadata.sections.single().title)
                assertEquals("Декодер", row.label)
                assertEquals(name, row.value)
            }
    }

    @Test fun absentOrBlankDecoderDoesNotCreateAnEmptyRow() {
        listOf(emptyMap(), mapOf("decoder_name" to ""), mapOf("decoder_name" to "   ")).forEach { values ->
            val metadata = videoMetadata(values + ("video_format" to "h264"), 0, null)
            assertFalse(metadata.sections.flatMap { it.rows }.any { it.label == "Декодер" })
        }
    }

    @Test fun decoderFieldDoesNotChangeTheVideoSummary() {
        val video = mapOf("video_format" to "H.264", "width" to "640", "height" to "360", "frame_rate" to "30")
        val before = videoMetadata(video, 1_000, 100_000)
        val after = videoMetadata(video + ("decoder_name" to "h264_mediacodec"), 1_000, 100_000)
        assertEquals(before.summary, after.summary)
        assertEquals(listOf("Видео", "Файл"), after.sections.map { it.title })
    }
}
