package com.solthariel.pawchan

import com.solthariel.pawchan.domain.settings.DnsProvider
import com.solthariel.pawchan.domain.settings.ServiceDns
import com.solthariel.pawchan.infrastructure.dns.ArhivachDnsSelector
import com.solthariel.pawchan.infrastructure.dns.isArhivachHost
import com.solthariel.pawchan.infrastructure.dns.withArhivachDns
import okhttp3.CookieJar
import okhttp3.Dns
import okhttp3.OkHttpClient
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test
import java.net.InetAddress
import java.net.UnknownHostException

class ArhivachDnsSelectorTest {
    private val systemAddress = InetAddress.getByAddress(byteArrayOf(1, 1, 1, 1))
    private val dohAddress = InetAddress.getByAddress(byteArrayOf(8, 8, 8, 8))
    private val custom = ServiceDns(DnsProvider.CUSTOM, "https://resolver.example/dns-query")

    @Test
    fun hostMatchingDoesNotIncludeUnrelatedOrLookalikeDomains() {
        assertTrue(isArhivachHost("arhivach.vc"))
        assertTrue(isArhivachHost("MEDIA.ARHIVACH.VC."))
        assertFalse(isArhivachHost("arhivach.vc.attacker.example"))
        assertFalse(isArhivachHost("notarhivach.vc"))
        assertFalse(isArhivachHost("vocaroo.com"))
        assertFalse(isArhivachHost("2ch.su"))
    }

    @Test
    fun defaultProfileUsesOriginalClientDns() {
        val client = OkHttpClient.Builder().dns { listOf(systemAddress) }.build()
        assertEquals(listOf(systemAddress), ArhivachDnsSelector(client).lookup("arhivach.vc"))
    }

    @Test
    fun selectedResolverHandlesOnlyArhivachHosts() {
        val client = OkHttpClient.Builder().dns { listOf(systemAddress) }.build()
        val dns = ArhivachDnsSelector(client, client.dns) { Dns { listOf(dohAddress) } }
        dns.update(custom)

        assertEquals(listOf(dohAddress), dns.lookup("arhivach.vc"))
        assertEquals(listOf(dohAddress), dns.lookup("media.arhivach.vc"))
        assertEquals(listOf(systemAddress), dns.lookup("2ch.su"))
        assertEquals(listOf(systemAddress), dns.lookup("media.vocaroo.com"))
    }

    @Test
    fun failedDohFallsBackToOriginalDns() {
        val client = OkHttpClient.Builder().dns { listOf(systemAddress) }.build()
        val dns = ArhivachDnsSelector(client, client.dns) { Dns { throw UnknownHostException("test") } }
        dns.update(custom)

        assertEquals(listOf(systemAddress), dns.lookup("arhivach.vc"))
    }

    @Test
    fun emptyDohResponseFallsBackToOriginalDns() {
        val client = OkHttpClient.Builder().dns { listOf(systemAddress) }.build()
        val dns = ArhivachDnsSelector(client, client.dns) { Dns { emptyList() } }
        dns.update(custom)

        assertEquals(listOf(systemAddress), dns.lookup("arhivach.vc"))
    }

    @Test
    fun unchangedProfileDoesNotRecreateResolverAndSystemCanBeRestored() {
        val client = OkHttpClient.Builder().dns { listOf(systemAddress) }.build()
        var creations = 0
        val dns =
            ArhivachDnsSelector(client, client.dns) { settings ->
                creations++
                if (settings.provider == DnsProvider.SYSTEM) null else Dns { listOf(dohAddress) }
            }
        dns.update(custom)
        dns.update(custom)
        assertEquals(1, creations)
        dns.update(ServiceDns())
        assertEquals(listOf(systemAddress), dns.lookup("arhivach.vc"))
    }

    @Test
    fun invalidCustomUrlDoesNotEnableDoh() {
        val client = OkHttpClient.Builder().dns { listOf(systemAddress) }.build()
        val dns = ArhivachDnsSelector(client)
        dns.update(ServiceDns(DnsProvider.CUSTOM, "http://resolver.example/dns-query"))

        assertEquals(listOf(systemAddress), dns.lookup("arhivach.vc"))
    }

    @Test
    fun backgroundClientRetainsCookiesAndNetworkPolicyWithoutChangingBaseClient() {
        val cookies = object : CookieJar by CookieJar.NO_COOKIES {}
        val client =
            OkHttpClient
                .Builder()
                .cookieJar(cookies)
                .followRedirects(false)
                .build()
        val configured = client.withArhivachDns(ServiceDns())

        assertTrue(configured.dns is ArhivachDnsSelector)
        assertFalse(client.dns is ArhivachDnsSelector)
        assertSame(client.dispatcher, configured.dispatcher)
        assertSame(client.connectionPool, configured.connectionPool)
        assertSame(cookies, configured.cookieJar)
        assertSame(client.sslSocketFactory, configured.sslSocketFactory)
        assertFalse(configured.followRedirects)
    }
}
