package com.solthariel.pawchan.data.network

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CatalogAttachmentStatisticsTest {
    private val decoder = CatalogPayloadDecoder(dvachReadJson())

    @Test
    fun preservesPawchanThreadWithTwoImagesAndOneReportedFile() {
        val thread = thread()

        assertEquals(2766787L, thread.address.thread.value)
        assertEquals("Pawchan", thread.subject)
        assertEquals(2, thread.attachments.size)
        assertEquals(2, thread.statistics.files)
        assertEquals(2, thread.statistics.images)
        assertEquals(3, thread.statistics.replies)
        assertFalse(thread.statistics.mediaBytesComplete)
    }

    @Test
    fun preservesAttachmentsWhenReportedFileCountIsZero() {
        val thread = thread("\"files_count\": 0")

        assertEquals(2, thread.statistics.files)
        assertEquals(2, thread.statistics.images)
        assertFalse(thread.statistics.mediaBytesComplete)
    }

    @Test
    fun preservesAttachmentsWhenFileCountIsMissing() {
        val thread = thread("")

        assertEquals(2, thread.statistics.files)
        assertFalse(thread.statistics.mediaBytesComplete)
    }

    @Test
    fun preservesAttachmentsWhenReportedFileCountIsNegative() {
        val thread = thread("\"files_count\": -1")

        assertEquals(2, thread.statistics.files)
        assertFalse(thread.statistics.mediaBytesComplete)
    }

    @Test
    fun keepsLargerReportedCountForFilesNotIncludedInCatalog() {
        val thread = thread("\"files_count\": 5")

        assertEquals(5, thread.statistics.files)
        assertEquals(2, thread.statistics.images)
        assertFalse(thread.statistics.mediaBytesComplete)
    }

    @Test
    fun completeMediaSizeRequiresConsistentCounterAndKnownSizes() {
        val thread = thread("\"files_count\": 2")

        assertEquals(2, thread.statistics.files)
        assertEquals(1675L * 1024L, thread.statistics.knownMediaBytes)
        assertTrue(thread.statistics.mediaBytesComplete)
    }

    @Test
    fun retainsThreadWhenOneAttachmentIsMalformed() {
        val payload = fixture().replace("/mobi/src/2766787/17923734742940186592.png", "invalid-path")
        val thread = requireNotNull(decoder.decode(payload)).threads.single()

        assertEquals(1, thread.attachments.size)
        assertEquals(1, thread.statistics.files)
        assertEquals(1, thread.statistics.images)
        assertFalse(thread.statistics.mediaBytesComplete)
    }

    @Test
    fun consistentStatisticsOnExistingCatalogFixtureStayUnchanged() {
        val payload = requireNotNull(javaClass.getResource("/fixtures/catalog-success.json")).readText()
        val thread = requireNotNull(decoder.decode(payload)).threads.single()

        assertEquals(2, thread.statistics.files)
        assertEquals(1, thread.statistics.images)
        assertTrue(thread.statistics.mediaBytesComplete)
    }

    private fun thread(counter: String = "\"files_count\": 1") =
        requireNotNull(
            decoder.decode(
                fixture().replace(
                    "\"files_count\": 1,",
                    counter.takeIf(String::isNotEmpty)?.plus(",").orEmpty(),
                ),
            ),
        ).threads.single()

    private fun fixture(): String {
        val resource = javaClass.getResource("/fixtures/catalog-multiple-op-files.json")
        return requireNotNull(resource).readText()
    }
}
