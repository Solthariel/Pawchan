package com.solthariel.pawchan.settings

import org.junit.Assert.assertEquals
import org.junit.Test

class AboutGroupsTest {
    @Test fun groupsOrderAppThenProjectThenTechnicalDetails() {
        val groups = aboutGroups(SettingsEnvironment(cacheSize = null, onClearCache = {}))
        assertEquals(listOf("Приложение", "Проект", "Техническая информация"), groups.map { it.title })
        assertEquals(listOf("Версия", "Проверить обновления"), groups[0].items.map { it.title })
        assertEquals(listOf("Релизы на GitHub"), groups[1].items.map { it.title })
        assertEquals(listOf("Версии компонентов", "Лицензии"), groups[2].items.map { it.title })
    }
}
