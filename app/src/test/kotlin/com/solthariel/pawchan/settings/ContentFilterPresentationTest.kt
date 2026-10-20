package com.solthariel.pawchan.settings

import com.solthariel.pawchan.domain.filter.ContentFilterMode
import com.solthariel.pawchan.domain.settings.AppSettings
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.ByteArrayInputStream
import java.nio.charset.CharacterCodingException

class ContentFilterPresentationTest {
    @Test
    fun emptyConditionHintExplainsTheSelectedModeInTheField() {
        val words = filterConditionPresentation(ContentFilterMode.ALL_WORDS.name)
        val phrase = filterConditionPresentation(ContentFilterMode.EXACT_PHRASE.name)
        assertNull(words.error)
        assertTrue(words.hint.contains("пробел или запятую"))
        assertTrue(words.hint.contains("Порядок не важен"))
        assertNull(phrase.error)
        assertTrue(phrase.hint.contains("фразу целиком"))
        assertNull(filterConditionPresentation(ContentFilterMode.EXACT_PHRASE.name).error)
    }

    @Test
    fun interfacePreferenceIsSearchableAndDoesNotReplaceOtherBehavior() {
        var current = AppSettings()
        val original = current
        val group = hiddenThreadsSettingsGroup(current) { update -> current = update(current) }
        val toggle = group.items.single() as SettingsItem.Toggle
        assertFalse(toggle.checked)
        toggle.onChange(true)
        assertTrue(current.behavior.showHiddenThreads)
        assertEquals(original.behavior.copy(showHiddenThreads = true), current.behavior)
        val found = searchSettings("Выводить скрытые", mapOf(SettingsSection.INTERFACE to listOf(group)))
        assertEquals("Интерфейс", found.single().title)
    }

    @Test
    fun unknownStatisticsAreNotDisplayedAsAnInventedZero() {
        assertNull(12L.orZeroWhenAvailable(false))
        assertNull((null as Long?).orZeroWhenAvailable(false))
        assertEquals(0L, (null as Long?).orZeroWhenAvailable(true))
        assertEquals(12L, 12L.orZeroWhenAvailable(true))
    }

    @Test
    fun utf8BomIsAcceptedWithoutDamagingCyrillic() {
        val text = "\uFEFF" + encodeContentFilterBackup(emptyList())
        val raw = readBackupText(ByteArrayInputStream(text.toByteArray(Charsets.UTF_8)))
        assertTrue(decodeContentFilterBackup(raw).isEmpty())
    }

    @Test(expected = IllegalArgumentException::class)
    fun oversizedInputStopsAtTheBoundInsteadOfReadingTheWholeFile() {
        readBackupText(ByteArrayInputStream(ByteArray(FILTER_BACKUP_MAX_BYTES + 1)))
    }

    @Test(expected = CharacterCodingException::class)
    fun invalidUtf8IsNotImportedAsReplacementCharacters() {
        readBackupText(ByteArrayInputStream(byteArrayOf(0xC3.toByte(), 0x28)))
    }
}
