package com.solthariel.pawchan.data.preferences

import androidx.datastore.preferences.core.mutablePreferencesOf
import androidx.datastore.preferences.core.stringPreferencesKey
import com.solthariel.pawchan.domain.settings.AppBehavior
import com.solthariel.pawchan.domain.settings.AppSettings
import com.solthariel.pawchan.domain.settings.DnsProvider
import com.solthariel.pawchan.domain.settings.ServiceDns
import com.solthariel.pawchan.domain.settings.ServiceDnsPreferences
import org.junit.Assert.assertEquals
import org.junit.Test

class ServiceDnsPreferencesTest {
    @Test
    fun oldVocarooKeysSurviveWithIndependentSystemDefaultForArhivach() {
        val stored =
            mutablePreferencesOf(
                stringPreferencesKey("app_vocaroo_dns") to "CUSTOM",
                stringPreferencesKey("app_vocaroo_dns_url") to "https://vocaroo.example/dns-query",
            )
        val settings = stored.toAppSettings().behavior.dns

        assertEquals(ServiceDns(DnsProvider.CUSTOM, "https://vocaroo.example/dns-query"), settings.vocaroo)
        assertEquals(ServiceDns(), settings.arhivach)
    }

    @Test
    fun bothProfilesRoundTripIndependently() {
        val profiles =
            ServiceDnsPreferences(
                vocaroo = ServiceDns(DnsProvider.XBOX),
                arhivach = ServiceDns(DnsProvider.CUSTOM, "https://archive.example/dns-query"),
            )
        val settings = AppSettings(behavior = AppBehavior(dns = profiles))
        val stored = mutablePreferencesOf().apply { write(settings) }

        assertEquals(settings, stored.toAppSettings())
        assertEquals("XBOX", stored[stringPreferencesKey("app_vocaroo_dns")])
        assertEquals("CUSTOM", stored[stringPreferencesKey("app_arhivach_dns")])
    }

    @Test
    fun unknownArhivachProviderFallsBackWithoutChangingVocaroo() {
        val stored =
            mutablePreferencesOf(
                stringPreferencesKey("app_arhivach_dns") to "UNKNOWN",
                stringPreferencesKey("app_vocaroo_dns") to "COMSS",
            )
        val profiles = stored.toAppSettings().behavior.dns

        assertEquals(DnsProvider.SYSTEM, profiles.arhivach.provider)
        assertEquals(DnsProvider.COMSS, profiles.vocaroo.provider)
    }

    @Test
    fun backupSchemaIncludesBothIndependentProfiles() {
        val original =
            mutablePreferencesOf().apply {
                write(
                    AppSettings(
                        behavior =
                            AppBehavior(
                                dns =
                                    ServiceDnsPreferences(
                                        arhivach = ServiceDns(DnsProvider.CUSTOM, "https://archive.example/dns-query"),
                                    ),
                            ),
                    ),
                )
            }
        val values = exportPortablePreferences(original)
        val restored = portableSchema().toMutablePreferences()
        writePortableValues(restored, validatePortablePreferences(values))

        assertEquals("CUSTOM", values["app_arhivach_dns"])
        assertEquals("https://archive.example/dns-query", values["app_arhivach_dns_url"])
        assertEquals(original.toAppSettings(), restored.toAppSettings())
    }
}
