package com.solthariel.pawchan.settings

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
    fun interfacePreferenceIsSearchableAndDoesNotReplaceOtherBehavior() {
        var current = AppSettings()
        val original = current
        val group = displaySettingsGroup(current) { update -> current = update(current) }
        assertEquals("Отображение", group.title)
        val toggle = group.items.single { it.title == "Выводить скрытые треды" } as SettingsItem.Toggle
        assertFalse(toggle.checked)
        toggle.onChange(true)
        assertTrue(current.behavior.showHiddenThreads)
        assertEquals(original.behavior.copy(showHiddenThreads = true), current.behavior)
        val found = searchSettings("Выводить скрытые", mapOf(SettingsSection.INTERFACE to listOf(group)))
        assertEquals("Интерфейс", found.single().title)
        assertEquals(2, found.single().items.size)
        val posts = found.single().items.single { it.title == "Выводить скрытые посты" } as SettingsItem.Toggle
        assertTrue(posts.checked)
        posts.onChange(false)
        assertEquals(original.behavior.copy(showHiddenThreads = true, showHiddenPosts = false), current.behavior)
    }

    @Test
    fun displayGroupsFourIndependentSwitchesBeforeTextControls() {
        var current = AppSettings(textSize = 15, collapseLines = 17)
        val original = current
        val group = displaySettingsGroup(current) { change -> current = change(current) }
        val switches = group.items.filterIsInstance<SettingsItem.Toggle>()
        val scales = group.items.filterIsInstance<SettingsItem.Scale>()
        assertEquals(
            listOf("Скрывать стикеры", "Скрывать ответы @monkey", "Выводить скрытые треды", "Выводить скрытые посты"),
            switches.map { it.title },
        )
        assertEquals(listOf("Размер текста", "Строк до сворачивания"), scales.map { it.title })
        assertEquals(switches + scales, group.items)
        assertEquals(listOf(false, false, false, true), switches.map { it.checked })
        switches.first().onChange(true)
        switches[1].onChange(true)
        assertEquals(original.copy(hideStickers = true, hideMonkeyReplies = true), current)
        val beforeText = current
        scales[0].onChange(16)
        scales[1].onChange(AppSettings.NO_COLLAPSE)
        assertEquals(beforeText.copy(textSize = 16, collapseLines = AppSettings.NO_COLLAPSE), current)
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
