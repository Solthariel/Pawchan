package com.solthariel.pawchan

import com.solthariel.pawchan.domain.settings.AppBehavior
import com.solthariel.pawchan.domain.settings.AppSettings
import com.solthariel.pawchan.domain.settings.DnsProvider
import com.solthariel.pawchan.domain.settings.ServiceDns
import com.solthariel.pawchan.domain.settings.ServiceDnsPreferences
import com.solthariel.pawchan.settings.SettingsItem
import com.solthariel.pawchan.settings.arhivachDnsItems
import com.solthariel.pawchan.settings.githubDnsItems
import com.solthariel.pawchan.settings.vocarooDnsItems
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ServiceDnsSettingsTest {
    @Test
    fun githubChoicePreservesForumAndOtherServicePreferences() {
        val before = AppSettings()
        var settings = before
        val choice = githubDnsItems(settings) { settings = it(settings) }.single() as SettingsItem.Choice
        assertEquals("DNS для GitHub", choice.title)
        choice.onSelect(DnsProvider.GEOHIDE.ordinal)

        assertEquals(DnsProvider.GEOHIDE, settings.behavior.dns.github.provider)
        assertEquals(before.behavior.client, settings.behavior.client)
        assertEquals(before.behavior.dns.arhivach, settings.behavior.dns.arhivach)
        assertEquals(before.behavior.dns.vocaroo, settings.behavior.dns.vocaroo)
    }

    @Test
    fun customGithubProfileHasIndependentUrlEditor() {
        val settings =
            AppSettings(behavior = AppBehavior(dns = ServiceDnsPreferences(github = ServiceDns(DnsProvider.CUSTOM))))
        assertEquals("Адрес DoH для GitHub", githubDnsItems(settings) {}.last().title)
        assertEquals(1, arhivachDnsItems(settings) {}.size)
        assertEquals(1, vocarooDnsItems(settings) {}.size)
    }

    @Test
    fun arhivachChoiceChangesOnlyArhivachProfile() {
        var settings = AppSettings()
        val choice = arhivachDnsItems(settings) { settings = it(settings) }.single() as SettingsItem.Choice
        assertEquals("DNS для Архивача", choice.title)
        assertEquals(0, choice.selected)
        choice.onSelect(DnsProvider.COMSS.ordinal)

        assertEquals(DnsProvider.COMSS, settings.behavior.dns.arhivach.provider)
        assertEquals(ServiceDns(), settings.behavior.dns.vocaroo)
    }

    @Test
    fun vocarooChoicePreservesArhivachProfile() {
        val archive = ServiceDns(DnsProvider.CUSTOM, "https://archive.example/dns-query")
        var settings = AppSettings(behavior = AppBehavior(dns = ServiceDnsPreferences(arhivach = archive)))
        val choice = vocarooDnsItems(settings) { settings = it(settings) }.single() as SettingsItem.Choice
        choice.onSelect(DnsProvider.XBOX.ordinal)

        assertEquals(DnsProvider.XBOX, settings.behavior.dns.vocaroo.provider)
        assertEquals(archive, settings.behavior.dns.arhivach)
    }

    @Test
    fun customArhivachProfileShowsItsOwnUrlEditor() {
        val settings =
            AppSettings(
                behavior = AppBehavior(dns = ServiceDnsPreferences(arhivach = ServiceDns(DnsProvider.CUSTOM))),
            )
        val items = arhivachDnsItems(settings) {}

        assertEquals(2, items.size)
        assertEquals("Адрес DoH для Архивача", items.last().title)
        assertTrue(items.last() is SettingsItem.Custom)
        assertEquals(1, vocarooDnsItems(settings) {}.size)
    }
}
