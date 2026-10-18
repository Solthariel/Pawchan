package com.solthariel.pawchan.settings

import com.solthariel.pawchan.domain.settings.AppSettings
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SettingsStandardTest {
    @Test fun storageHasOneLimitAndOneActionWithUsedSize() {
        val items = storageGroups(AppSettings(), {}, SettingsEnvironment("399 КБ", {})).single().items
        assertEquals(3, items.size)
        assertEquals(1, items.count { it.title.startsWith("Очистить кэш") })
        assertTrue(items.any { it.title == "Очистить кэш — 399 КБ" })
        assertFalse(items.any { it.title == "Размер временного кэша" })
    }

    @Test fun androidLimitIsNotAnInertPreference() {
        val items = automationGroups(AppSettings(), {}).flatMap { it.items }
        assertFalse(items.any { it.title == "Ограничения Android" })
    }

    @Test fun notificationRecoveryUsesTheAvailableSystemAction() {
        assertEquals(NotificationAccessAction.NONE, notificationAccessAction(33, true, true, false))
        assertEquals(NotificationAccessAction.REQUEST, notificationAccessAction(33, false, false, true))
        assertEquals(NotificationAccessAction.SETTINGS, notificationAccessAction(33, false, false, false))
        assertEquals(NotificationAccessAction.SETTINGS, notificationAccessAction(33, true, false, false))
        assertEquals(NotificationAccessAction.SETTINGS, notificationAccessAction(32, false, false, true))
    }
}
