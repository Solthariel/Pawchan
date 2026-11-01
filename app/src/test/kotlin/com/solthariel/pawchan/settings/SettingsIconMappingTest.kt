package com.solthariel.pawchan.settings

import com.solthariel.pawchan.core.ui.UiIconKind
import org.junit.Assert.assertEquals
import org.junit.Test

class SettingsIconMappingTest {
    @Test fun networkAndAutohideUseTheRequestedSymbols() {
        assertEquals(UiIconKind.PUBLIC, SettingsSection.NETWORK.icon)
        assertEquals(UiIconKind.CLEANING, SettingsSection.AUTOHIDE.icon)
    }

    @Test fun mediaKeepsItsPreviousImageIcon() {
        assertEquals(UiIconKind.IMAGE, SettingsSection.MEDIA.icon)
    }

    @Test fun postingAndStorageDoNotReuseGenericTune() {
        assertEquals(UiIconKind.SEND, SettingsSection.POSTING.icon)
        assertEquals(UiIconKind.STORAGE, SettingsSection.STORAGE.icon)
        assertEquals(UiIconKind.FORUM, SettingsSection.THREADS.icon)
    }

    @Test fun settingsSectionsHaveDistinctIconsAndKeepTheirTitles() {
        assertEquals(
            SettingsSection.entries.size,
            SettingsSection.entries
                .map { it.icon }
                .toSet()
                .size,
        )
        assertEquals("Медиа", SettingsSection.MEDIA.title)
        assertEquals("Отправка", SettingsSection.POSTING.title)
        assertEquals("Хранилище", SettingsSection.STORAGE.title)
    }
}
