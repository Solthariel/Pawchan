package com.solthariel.pawchan.data.preferences

import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.mutablePreferencesOf
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import com.solthariel.pawchan.domain.settings.AttachmentDefaults
import com.solthariel.pawchan.domain.settings.ClientPreferences
import com.solthariel.pawchan.domain.settings.ComposerField
import com.solthariel.pawchan.domain.settings.ComposerPreferences
import com.solthariel.pawchan.domain.settings.ImageEncodingDefaults
import com.solthariel.pawchan.domain.settings.MediaPreferences
import com.solthariel.pawchan.domain.settings.NavigationPreferences
import com.solthariel.pawchan.domain.settings.ReadingPreferences
import com.solthariel.pawchan.domain.settings.ReencodeFormat
import com.solthariel.pawchan.domain.settings.UploadNameMode
import com.solthariel.pawchan.domain.settings.UploadNaming
import com.solthariel.pawchan.domain.settings.VideoCompletion
import org.junit.Assert.assertEquals
import org.junit.Test

class ClientPreferencesStoreTest {
    @Test
    fun oldInstallKeepsCompatibleDefaults() {
        assertEquals(ClientPreferences(), emptyPreferences().readClientPreferences())
    }

    @Test
    fun allNewOptionsRoundTripTogether() {
        val expected =
            ClientPreferences(
                composer =
                    ComposerPreferences(
                        hiddenFields = setOf(ComposerField.EMAIL, ComposerField.SUBJECT),
                        attachments =
                            AttachmentDefaults(
                                uniqueHash = true,
                                removeMetadata = true,
                                spoiler = true,
                                naming = UploadNaming(UploadNameMode.CUSTOM, "кот"),
                                reencoding = ImageEncodingDefaults(ReencodeFormat.PNG, 72, 4),
                            ),
                    ),
                media = MediaPreferences(false, true, VideoCompletion.REPEAT, true, true),
                reading = ReadingPreferences(false, false, NavigationPreferences(2, 15, 20)),
                connection =
                    com.solthariel.pawchan.domain.settings
                        .ForumConnectionPreferences("2ch.org"),
            )
        val stored = mutablePreferencesOf()
        stored.writeClientPreferences(expected)
        assertEquals(expected, stored.readClientPreferences())
    }

    @Test
    fun malformedNumbersAreClampedAndUnknownEnumsDoNotCrash() {
        val stored =
            mutablePreferencesOf(
                intPreferencesKey("attachment_quality") to -5,
                intPreferencesKey("attachment_divisor") to 3,
                stringPreferencesKey("attachment_reencode") to "FUTURE_FORMAT",
                stringPreferencesKey("attachment_name_mode") to "FUTURE_NAME",
                stringSetPreferencesKey("composer_hidden_fields") to setOf("EMAIL", "FUTURE_FIELD"),
            )
        val restored = stored.readClientPreferences()
        assertEquals(1, restored.composer.attachments.reencoding.quality)
        assertEquals(1, restored.composer.attachments.reencoding.divisor)
        assertEquals(ReencodeFormat.ORIGINAL, restored.composer.attachments.reencoding.format)
        assertEquals(UploadNameMode.ORIGINAL, restored.composer.attachments.naming.mode)
        assertEquals(setOf(ComposerField.EMAIL), restored.composer.hiddenFields)
    }

    @Test
    fun writingOneSettingDoesNotEraseUnrelatedPreferences() {
        val key = stringPreferencesKey("unrelated")
        val stored = mutablePreferencesOf(key to "keep")
        stored.writeClientPreferences(ClientPreferences())
        assertEquals("keep", stored[key])
    }
}
