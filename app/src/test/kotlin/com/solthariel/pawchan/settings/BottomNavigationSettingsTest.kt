package com.solthariel.pawchan.settings

import com.solthariel.pawchan.domain.settings.AppSettings
import org.junit.Assert.assertEquals
import org.junit.Test

class BottomNavigationSettingsTest {
    @Test fun scaleDefaultsToTwentyAndUpdatesTheLatestSettingsWithoutResettingOtherValues() {
        var current = AppSettings()
        val group = bottomNavigationSettingsGroup(current) { transform -> current = transform(current) }
        val scale = group.items.single() as SettingsItem.Scale
        assertEquals("Прозрачность нижней панели", scale.title)
        assertEquals(20, scale.value)
        assertEquals(0..100, scale.range)
        assertEquals("%", scale.unit)
        current = current.copy(textSize = 16)
        scale.onChange(37)
        assertEquals(16, current.textSize)
        assertEquals(37, current.behavior.client.reading.navigation.bottomBarTransparencyPercent)
        scale.onChange(101)
        assertEquals(100, current.behavior.client.reading.navigation.bottomBarTransparencyPercent)
    }
}
