package com.solthariel.pawchan.settings

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ChangelogContentTest {
    @Test fun markersAndInternalReleaseHeaderAreNotRenderedAsText() {
        val sections = parseChangelog("Версия 0.1.0 — выпуск №1\n\n* Исправлены ответы\n* Убрана вспышка\n")
        assertEquals(listOf(ChangelogSection("Изменения", listOf("Исправлены ответы", "Убрана вспышка"))), sections)
    }

    @Test fun headingsAndEntriesKeepTheirOrderWithoutEmptyGroups() {
        val sections =
            parseChangelog(
                "# Интерфейс\n- Ответы\n• Настройки\n\nПустая группа\nБазовая версия:\n* Чтение тредов",
            )
        assertEquals(
            listOf(
                ChangelogSection("Интерфейс", listOf("Ответы", "Настройки")),
                ChangelogSection("Базовая версия", listOf("Чтение тредов")),
            ),
            sections,
        )
    }

    @Test fun emptyFileHasNoPhantomEntries() {
        assertTrue(parseChangelog("\n\nВерсия 0.1.0\n").isEmpty())
    }
}
