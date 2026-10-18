package com.solthariel.pawchan.data.preferences

import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.mutablePreferencesOf
import androidx.datastore.preferences.core.stringPreferencesKey
import com.solthariel.pawchan.domain.settings.AppSettings
import com.solthariel.pawchan.domain.settings.AutomationPreferences
import com.solthariel.pawchan.domain.settings.DownloadNaming
import com.solthariel.pawchan.domain.settings.DownloadPreferences
import com.solthariel.pawchan.domain.settings.NotificationPreferences
import com.solthariel.pawchan.domain.settings.StoragePreferences
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class AutomationPreferencesTest {
    @Test fun automationAndDownloadsRoundTrip() {
        val stored = mutablePreferencesOf()
        val automation =
            AutomationPreferences(
                true,
                60,
                false,
                false,
                180,
                true,
                NotificationPreferences(false, false),
            )
        stored.writeAutomation(automation)
        stored.writeDownloads(DownloadPreferences("content://tree/folder", DownloadNaming.DETAILED))
        stored.writeStorage(StoragePreferences(1024))
        assertEquals(automation, stored.readAutomation())
        assertEquals(DownloadNaming.DETAILED, stored.readDownloads().naming)
        assertEquals(1024, stored.readStorage().cacheMb)
    }

    @Test fun malformedIntervalsAndLimitsFallBack() {
        val stored =
            mutablePreferencesOf(
                intPreferencesKey("auto_open_seconds") to -1,
                intPreferencesKey("auto_background_minutes") to 1,
                intPreferencesKey("storage_cache_mb") to Int.MAX_VALUE,
            )
        assertEquals(30, stored.readAutomation().openIntervalSeconds)
        assertEquals(15, stored.readAutomation().backgroundMinutes)
        assertEquals(StoragePreferences.DEFAULT_MB, stored.readStorage().cacheMb)
    }

    @Test fun portableSettingsExcludeLocalGrantsAndUnknownSecrets() {
        val stored = mutablePreferencesOf()
        stored.write(AppSettings())
        stored[stringPreferencesKey("download_tree")] = "content://private/folder"
        stored[stringPreferencesKey("session_cookie")] = "secret"
        val portable = exportPortablePreferences(stored)
        assertFalse(portable.containsKey("download_tree"))
        assertFalse(portable.containsKey("session_cookie"))
        val restored = mutablePreferencesOf()
        copyPortableTo(restored, validatePortablePreferences(portable))
        assertEquals(
            stored
                .toAppSettings()
                .behavior.client.automation,
            restored
                .toAppSettings()
                .behavior.client.automation,
        )
    }

    @Test(expected = IllegalArgumentException::class)
    fun unknownPortableKeysRejected() {
        validatePortablePreferences(mapOf("session_cookie" to "bad"))
    }

    @Test(expected = IllegalArgumentException::class)
    fun invalidBooleanRejectedBeforeApplying() {
        validatePortablePreferences(mapOf("auto_wifi_only" to "not-a-boolean"))
    }
}
