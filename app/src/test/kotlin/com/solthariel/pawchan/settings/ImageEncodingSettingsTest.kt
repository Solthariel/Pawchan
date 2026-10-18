package com.solthariel.pawchan.settings

import com.solthariel.pawchan.domain.settings.AttachmentDefaults
import com.solthariel.pawchan.domain.settings.ImageEncodingDefaults
import com.solthariel.pawchan.domain.settings.ReencodeFormat
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ImageEncodingSettingsTest {
    @Test fun qualityWarningIsAbsentForEveryFormatButControlsRemain() {
        ReencodeFormat.entries.forEach { format ->
            val value = AttachmentDefaults(reencoding = ImageEncodingDefaults(format))
            val titles = imageEncodingGroup(value) {}.items.map { it.title }
            assertFalse("Возможна потеря качества" in titles)
            assertTrue("Формат" in titles)
            assertEquals(format != ReencodeFormat.ORIGINAL, "Размер изображения" in titles)
            assertEquals(format == ReencodeFormat.JPEG, "Качество JPEG" in titles)
        }
    }

    @Test fun formatAndSizeActionsStillUpdateOnlyTheirOwnPreference() {
        var value = AttachmentDefaults(reencoding = ImageEncodingDefaults(ReencodeFormat.JPEG, 85, 1))
        val change: ((AttachmentDefaults) -> AttachmentDefaults) -> Unit = { value = it(value) }
        val items = imageEncodingGroup(value, change).items
        val size = items.single { it.title == "Размер изображения" } as SettingsItem.Choice
        size.onSelect(1)
        assertEquals(ImageEncodingDefaults(ReencodeFormat.JPEG, 85, 2), value.reencoding)
        val format = items.single { it.title == "Формат" } as SettingsItem.Choice
        format.onSelect(ReencodeFormat.PNG.ordinal)
        assertEquals(ImageEncodingDefaults(ReencodeFormat.PNG, 85, 2), value.reencoding)
    }

    @Test fun collapseSettingUsesTheGenitiveAfterItsLabel() {
        assertEquals("21 строки", settingsScaleLabel(21, "строк"))
        assertEquals("22 строк", settingsScaleLabel(22, "строк"))
        assertEquals("11 строк", settingsScaleLabel(11, "строк"))
        assertEquals("16 sp", settingsScaleLabel(16, "sp"))
    }
}
