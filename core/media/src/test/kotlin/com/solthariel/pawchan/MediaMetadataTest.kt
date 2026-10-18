package com.solthariel.pawchan

import com.solthariel.pawchan.media.metadata.bitrateLabel
import com.solthariel.pawchan.media.metadata.decimalLabel
import com.solthariel.pawchan.media.metadata.durationLabel
import com.solthariel.pawchan.media.metadata.exifDateLabel
import com.solthariel.pawchan.media.metadata.gifFrames
import com.solthariel.pawchan.media.metadata.imageHeaderInfo
import com.solthariel.pawchan.media.metadata.imageSummary
import com.solthariel.pawchan.media.metadata.sampleRateLabel
import com.solthariel.pawchan.media.metadata.shortCodecName
import com.solthariel.pawchan.media.metadata.videoMetadata
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.io.ByteArrayOutputStream

class MediaMetadataTest {
    @Test fun videoSummaryUsesShortCodecNames() {
        val values =
            mapOf(
                "video_format" to "H.264 / AVC / MPEG-4 AVC / MPEG-4 part 10",
                "width" to "360",
                "height" to "640",
                "frame_rate" to "30.000",
                "audio_format" to "AAC (Advanced Audio Coding)",
                "channels" to "2",
                "sample_rate" to "48000",
                "pixel_format" to "yuv420p",
            )
        val metadata = videoMetadata(values, durationMillis = 49_000, bytes = 3_670_016)
        assertEquals("H.264 · 360×640 · 3.7 МБ · 30 FPS · 0:49", metadata.summary)
        assertEquals(listOf("Видео", "Аудио", "Файл"), metadata.sections.map { it.title })
        assertEquals(false, metadata.sections.flatMap { it.rows }.any { it.value == "yuv420p" })
    }

    @Test fun silentVideoHasNoAudioSection() {
        val metadata = videoMetadata(mapOf("video_format" to "Google VP9", "width" to "1", "height" to "1"), 0, null)
        assertEquals(listOf("Видео"), metadata.sections.map { it.title })
        assertEquals("Google VP9 · 1×1", metadata.summary)
    }

    @Test fun imageSummaryShowsSizeAndFramesOnce() {
        assertEquals("JPEG · 702×682 · 46 КБ", imageSummary("JPEG", "702×682", 46_080, null, null))
        assertEquals("GIF · 375×500 · 748 КБ · 8 кадров", imageSummary("GIF", "375×500", 748_544, 8, null))
        assertEquals("JPEG · 10×10 · 3.3 МБ · Pixel 7", imageSummary("JPEG", "10×10", 3_276_800, null, "Pixel 7"))
    }

    @Test fun formatsNumbers() {
        assertEquals("44.1 кГц", sampleRateLabel(44_100))
        assertEquals("850 кбит/с", bitrateLabel(106_250, 1_000))
        assertEquals("1.2 Мбит/с", bitrateLabel(150_000, 1_000))
        assertNull(bitrateLabel(1, 0))
        assertEquals("1:01:05", durationLabel(3_665_000))
        assertEquals("H.264", shortCodecName("H.264 / AVC"))
        assertEquals("H.264", shortCodecName("H.264 Android MediaCodec decoder"))
        assertEquals("VP9", shortCodecName("VP9 decoder"))
        assertEquals("29.92", decimalLabel(29.916))
        assertEquals("03.05.2021 14:22:01", exifDateLabel("2021:05:03 14:22:01"))
    }

    @Test fun countsGifFrames() {
        assertEquals(3, gifFrames(gif(frames = 3)))
        assertNull(gifFrames(byteArrayOf(1, 2)))
    }

    @Test fun readsPngColorAndApngFrames() {
        val ihdr = byteArrayOf(0, 0, 0, 1, 0, 0, 0, 1, 8, 6, 0, 0, 0)
        val actl = byteArrayOf(0, 0, 0, 12, 0, 0, 0, 0)
        val png = PNG + chunk("IHDR", ihdr) + chunk("acTL", actl) + chunk("IDAT", byteArrayOf(0))
        val info = imageHeaderInfo(png)
        assertEquals(8, info.bitDepth)
        assertEquals("RGBA", info.colorType)
        assertEquals(12, info.frames)
    }

    @Test fun countsWebpAnimationFrames() {
        val body =
            "WEBP".toByteArray() + riff("VP8X", ByteArray(10)) + riff("ANMF", ByteArray(3)) +
                riff("ANMF", ByteArray(4))
        val webp = "RIFF".toByteArray() + le(body.size) + body
        assertEquals(2, imageHeaderInfo(webp).frames)
        val still = "RIFF".toByteArray() + le(8) + "WEBP".toByteArray() + riff("VP8L", ByteArray(2))
        assertEquals("без потерь", imageHeaderInfo(still).compression)
    }

    private fun gif(frames: Int): ByteArray {
        val out = ByteArrayOutputStream()
        out.write("GIF89a".toByteArray())
        // Логический экран 1×1 с глобальной палитрой из двух цветов.
        out.write(byteArrayOf(1, 0, 1, 0, 0x80.toByte(), 0, 0))
        out.write(ByteArray(6))
        repeat(frames) {
            out.write(byteArrayOf(0x21, 0xF9.toByte(), 4, 0, 10, 0, 0, 0))
            out.write(byteArrayOf(0x2C, 0, 0, 0, 0, 1, 0, 1, 0, 0))
            out.write(byteArrayOf(2, 2, 0x44, 0x01, 0))
        }
        out.write(0x3B)
        return out.toByteArray()
    }

    private fun chunk(
        type: String,
        data: ByteArray,
    ): ByteArray = be(data.size) + type.toByteArray() + data + ByteArray(4)

    private fun riff(
        type: String,
        data: ByteArray,
    ): ByteArray = type.toByteArray() + le(data.size) + data + ByteArray(data.size % 2)

    private fun be(value: Int) = ByteArray(4) { (value shr (24 - 8 * it)).toByte() }

    private fun le(value: Int) = be(value).reversedArray()

    private companion object {
        val PNG = byteArrayOf(0x89.toByte(), 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A)
    }
}
