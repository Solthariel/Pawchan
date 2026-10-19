package com.solthariel.pawchan

import com.solthariel.pawchan.domain.settings.DnsProvider
import com.solthariel.pawchan.domain.settings.ServiceDns
import com.solthariel.pawchan.infrastructure.dns.customDohUrl
import com.solthariel.pawchan.infrastructure.dns.dohEndpoint
import com.solthariel.pawchan.infrastructure.vocaroo.isVocarooHost
import com.solthariel.pawchan.infrastructure.vocaroo.vocarooUsesCronet
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class VocarooNetworkPolicyTest {
    @Test fun dnsSettingAppliesOnlyToVocarooHosts() {
        assertTrue(isVocarooHost("vocaroo.com"))
        assertTrue(isVocarooHost("voca.ro"))
        assertTrue(isVocarooHost("media.vocaroo.com"))
        assertTrue(isVocarooHost("MEDIA1.vocaroo.com."))
        assertFalse(isVocarooHost("2ch.hk"))
        assertFalse(isVocarooHost("notvocaroo.com"))
    }

    @Test fun presetsCarryBootstrapAddressesAndCustomUrlIsHttpsOnly() {
        assertNull(dohEndpoint(ServiceDns()))
        val comss = requireNotNull(dohEndpoint(ServiceDns(DnsProvider.COMSS)))
        assertEquals("dns.comss.one", comss.url.host)
        assertTrue(comss.bootstrap.isNotEmpty())
        assertEquals("xbox-dns.ru", dohEndpoint(ServiceDns(DnsProvider.XBOX))?.url?.host)
        assertEquals("dns.geohide.ru", dohEndpoint(ServiceDns(DnsProvider.GEOHIDE))?.url?.host)
        assertNull(customDohUrl("http://dns.example/dns-query"))
        assertNull(customDohUrl("not a url"))
        assertEquals("dns.example", customDohUrl(" https://dns.example/dns-query ")?.host)
        assertNull(dohEndpoint(ServiceDns(DnsProvider.CUSTOM, "http://dns.example")))
    }

    @Test fun cronetAlternatesWithOkHttpOnlyWithSystemDns() {
        assertTrue(vocarooUsesCronet(1, systemDns = true))
        assertFalse(vocarooUsesCronet(2, systemDns = true))
        assertTrue(vocarooUsesCronet(3, systemDns = true))
        assertFalse(vocarooUsesCronet(1, systemDns = false))
    }
}
