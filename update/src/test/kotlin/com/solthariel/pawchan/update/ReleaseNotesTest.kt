package com.solthariel.pawchan.update

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ReleaseNotesTest {
    @Test fun downloadedNotesHaveNoRawMarkersOrRepeatedVersionTitle() {
        val parsed = parseReleaseNotes("Версия 0.1.4\n\nИсправления\n\n* Первый пункт\n* Второй пункт\n")
        assertEquals(listOf(ReleaseNotesSection("Исправления", listOf("Первый пункт", "Второй пункт"))), parsed)
    }

    @Test fun mixedListMarkersProduceTheSameEntries() {
        val parsed = parseReleaseNotes("Улучшения\n* Один\n- Два\n• Три")
        assertEquals(listOf("Один", "Два", "Три"), parsed.single().entries)
    }

    @Test fun markdownVersionHeaderAndBomAreNotASection() {
        val parsed = parseReleaseNotes("\uFEFF# Версия 0.1.5\r\n\r\n## Исправления:\r\n- Ответы")
        assertEquals(listOf(ReleaseNotesSection("Исправления", listOf("Ответы"))), parsed)
    }

    @Test fun englishVersionHeaderIsAlsoMetadata() {
        val parsed = parseReleaseNotes("Version 0.1.5\n* Fixed")
        assertEquals(listOf(ReleaseNotesSection("Изменения", listOf("Fixed"))), parsed)
    }

    @Test fun emptyAndHeaderOnlyFilesDoNotCreateEmptySections() {
        assertTrue(parseReleaseNotes(" \n").isEmpty())
        assertTrue(parseReleaseNotes("Версия 0.1.5\n\nИсправления").isEmpty())
    }

    @Test fun sectionOrderAndParagraphsWithinItemsArePreserved() {
        val parsed = parseReleaseNotes("Исправления\n* Строка один\n  строка два\n\nУлучшения\n* Последний")
        assertEquals(listOf("Исправления", "Улучшения"), parsed.map { it.title })
        assertEquals("Строка один строка два", parsed.first().entries.single())
        assertFalse(parsed.any { it.title.startsWith("Версия") })
    }
}
