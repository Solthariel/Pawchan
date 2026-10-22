package com.solthariel.pawchan

import com.solthariel.pawchan.domain.settings.DnsProvider
import com.solthariel.pawchan.domain.settings.ServiceDns
import com.solthariel.pawchan.infrastructure.dns.GithubDnsSelector
import com.solthariel.pawchan.infrastructure.dns.isGithubHost
import okhttp3.Dns
import okhttp3.OkHttpClient
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.net.InetAddress
import java.net.UnknownHostException

class GithubDnsSelectorTest {
    private val systemAddress = InetAddress.getByAddress(byteArrayOf(1, 1, 1, 1))
    private val dohAddress = InetAddress.getByAddress(byteArrayOf(8, 8, 8, 8))
    private val custom = ServiceDns(DnsProvider.CUSTOM, "https://resolver.example/dns-query")

    @Test
    fun hostMatchingDoesNotIncludeUnrelatedOrLookalikeDomains() {
        assertTrue(isGithubHost("github.com"))
        assertTrue(isGithubHost("raw.githubusercontent.com"))
        assertTrue(isGithubHost("release-assets.githubusercontent.com"))
        assertFalse(isGithubHost("githubusercontent.com.attacker.example"))
        assertTrue(isGithubHost("API.GITHUB.COM."))
        assertFalse(isGithubHost("github.com.attacker.example"))
        assertFalse(isGithubHost("notgithub.com"))
        assertFalse(isGithubHost("vocaroo.com"))
        assertFalse(isGithubHost("2ch.su"))
    }

    @Test
    fun defaultProfileUsesOriginalClientDns() {
        val client = OkHttpClient.Builder().dns { listOf(systemAddress) }.build()
        assertEquals(listOf(systemAddress), GithubDnsSelector(client).lookup("github.com"))
    }

    @Test
    fun selectedResolverHandlesOnlyGithubHosts() {
        val client = OkHttpClient.Builder().dns { listOf(systemAddress) }.build()
        val dns = GithubDnsSelector(client, client.dns) { Dns { listOf(dohAddress) } }
        dns.update(custom)

        assertEquals(listOf(dohAddress), dns.lookup("github.com"))
        assertEquals(listOf(dohAddress), dns.lookup("media.github.com"))
        assertEquals(listOf(systemAddress), dns.lookup("2ch.su"))
        assertEquals(listOf(systemAddress), dns.lookup("media.vocaroo.com"))
    }

    @Test
    fun failedDohFallsBackToOriginalDns() {
        val client = OkHttpClient.Builder().dns { listOf(systemAddress) }.build()
        val dns = GithubDnsSelector(client, client.dns) { Dns { throw UnknownHostException("test") } }
        dns.update(custom)

        assertEquals(listOf(systemAddress), dns.lookup("github.com"))
    }

    @Test
    fun emptyDohResponseFallsBackToOriginalDns() {
        val client = OkHttpClient.Builder().dns { listOf(systemAddress) }.build()
        val dns = GithubDnsSelector(client, client.dns) { Dns { emptyList() } }
        dns.update(custom)

        assertEquals(listOf(systemAddress), dns.lookup("github.com"))
    }

    @Test
    fun unchangedProfileDoesNotRecreateResolverAndSystemCanBeRestored() {
        val client = OkHttpClient.Builder().dns { listOf(systemAddress) }.build()
        var creations = 0
        val dns =
            GithubDnsSelector(client, client.dns) { settings ->
                creations++
                if (settings.provider == DnsProvider.SYSTEM) null else Dns { listOf(dohAddress) }
            }
        dns.update(custom)
        dns.update(custom)
        assertEquals(1, creations)
        dns.update(ServiceDns())
        assertEquals(listOf(systemAddress), dns.lookup("github.com"))
    }

    @Test
    fun invalidCustomUrlDoesNotEnableDoh() {
        val client = OkHttpClient.Builder().dns { listOf(systemAddress) }.build()
        val dns = GithubDnsSelector(client)
        dns.update(ServiceDns(DnsProvider.CUSTOM, "http://resolver.example/dns-query"))

        assertEquals(listOf(systemAddress), dns.lookup("github.com"))
    }
}
