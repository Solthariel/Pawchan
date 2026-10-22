package com.solthariel.pawchan.data.network

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Base64

class PostingPngTrailerTest {
    private val image =
        Base64.getDecoder().decode(
            "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mP8/x8AAwMCAO+jF1kAAAAASUVORK5CYII=",
        )
    private val trailer = "Samsung-like-private-trailer".toByteArray()

    @Test fun hashOnlyEditPreservesTrailerAndDoesNotMutateOriginal() {
        val source = image + trailer
        val saved = source.copyOf()
        val result = PostingMediaBytes.unique(source)
        assertArrayEquals(saved, source)
        assertArrayEquals(trailer, result.takeLast(trailer.size).toByteArray())
        assertTrue(result.size > source.size)
    }

    @Test fun metadataRemovalDropsTrailerAndHashRemainsCompatible() {
        val source = image + trailer
        assertArrayEquals(image, PostingMediaBytes.removeMetadata(source))
        val hashed = PostingMediaBytes.unique(source)
        val cleaned = PostingMediaBytes.removeMetadata(hashed)
        assertArrayEquals(image.takeLast(12).toByteArray(), cleaned.takeLast(12).toByteArray())
    }

    @Test fun truncatedOrInvalidChunkIsStillRejectedRatherThanUploaded() {
        assertThrows(IllegalArgumentException::class.java) { PostingMediaBytes.unique(image.copyOf(20)) }
        val broken = image.copyOf()
        broken[8] = 0x7f
        assertThrows(IllegalArgumentException::class.java) { PostingMediaBytes.removeMetadata(broken + trailer) }
    }
}
