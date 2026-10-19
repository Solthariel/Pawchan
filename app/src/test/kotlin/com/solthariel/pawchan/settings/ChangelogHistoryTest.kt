package com.solthariel.pawchan.settings

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.IOException

class ChangelogHistoryTest {
    private val metadata =
        """
        {"versions":[
        {"code":1,"name":"0.1.0","date":"01.10.2026"},
        {"code":3,"name":"0.1.2","date":"18.10.2026"},
        {"code":2,"name":"0.1.1","date":"18.10.2026"}]}
        """.trimIndent()

    @Test
    fun newestReleaseComesFirstEvenWhenMetadataIsNotSorted() {
        val releases = readChangelogHistory(3, assets())
        assertEquals(listOf(3, 2, 1), releases.map { it.code })
        assertEquals(listOf("0.1.2", "0.1.1", "0.1.0"), releases.map { it.version })
        assertEquals("01.10.2026", releases.last().date)
    }

    @Test
    fun futureReleaseNotesAreNotLoadedOrDisplayed() {
        val paths = mutableListOf<String>()
        val read = assets()
        val releases =
            readChangelogHistory(2) { path ->
                paths.add(path)
                read(path)
            }
        assertEquals(listOf(2, 1), releases.map { it.code })
        assertFalse(paths.any { it.endsWith("/3.txt") })
    }

    @Test
    fun eachReleaseKeepsItsOwnSectionsAndEntries() {
        val releases = readChangelogHistory(3, assets())
        releases.forEach { release ->
            assertEquals(
                listOf(ChangelogSection("Исправления", listOf("Запись ${release.code}"))),
                release.sections,
            )
        }
    }

    @Test
    fun firstVersionAlsoHasACompleteHistory() {
        val releases = readChangelogHistory(1, assets())
        assertEquals(listOf(1), releases.map { it.code })
        assertTrue(releases.single().sections.isNotEmpty())
    }

    @Test(expected = IllegalArgumentException::class)
    fun duplicateCodesAreRejectedInsteadOfReusingLazyListKeys() {
        readChangelogHistory(3, assets(metadata.replace("\"code\":2", "\"code\":1")))
    }

    @Test(expected = IllegalArgumentException::class)
    fun missingInstalledVersionIsReported() {
        readChangelogHistory(4, assets())
    }

    @Test(expected = IllegalArgumentException::class)
    fun missingMetadataFieldIsReported() {
        readChangelogHistory(3, assets(metadata.replace("\"name\":\"0.1.1\",", "")))
    }

    @Test(expected = IllegalArgumentException::class)
    fun malformedIndexIsReported() {
        readChangelogHistory(3, assets("not JSON"))
    }

    @Test(expected = IllegalArgumentException::class)
    fun emptyReleaseNotesAreReportedInsteadOfShowingABlankVersion() {
        readChangelogHistory(3) { path -> if (path.endsWith(".json")) metadata else "Версия 0.1.0\n" }
    }

    @Test(expected = IOException::class)
    fun missingBundledNotesAreNotSilentlyOmitted() {
        readChangelogHistory(3) { path ->
            if (path.endsWith(".json")) metadata else throw IOException("Missing bundled notes")
        }
    }

    private fun assets(index: String = metadata): (String) -> String =
        { path ->
            if (path == "changelog-history/versions.json") {
                index
            } else {
                val code = path.substringAfterLast('/').removeSuffix(".txt")
                "Версия 0.1.0\n\nИсправления\n\n* Запись $code\n"
            }
        }
}
