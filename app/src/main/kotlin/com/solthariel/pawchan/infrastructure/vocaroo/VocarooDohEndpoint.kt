package com.solthariel.pawchan.infrastructure.vocaroo

import com.solthariel.pawchan.domain.settings.VocarooDns
import com.solthariel.pawchan.domain.settings.VocarooDnsProvider
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import java.net.InetAddress

/** Адрес DoH и IP-адреса самого DoH-сервера, чтобы не спрашивать о нём системный DNS. */
internal data class VocarooDohEndpoint(
    val url: HttpUrl,
    val bootstrap: List<InetAddress>,
)

/**
 * Хосты, для которых действует настройка «DNS для Vocaroo» (vocaroo.com, voca.ro, media.vocaroo.com
 * и зеркала media1…); остальные запросы её не видят.
 */
internal fun isVocarooHost(host: String): Boolean {
    val name = host.lowercase().trimEnd('.')
    return name == "vocaroo.com" || name == "voca.ro" || name.endsWith(".vocaroo.com")
}

/**
 * Пресеты взяты с официальных страниц сервисов (Comss, XBOX DNS); адреса GeoHide на странице
 * не перечислены и получены разрешением dns.geohide.ru — каждый проверен DoH-запросом.
 */
internal fun vocarooDohEndpoint(dns: VocarooDns): VocarooDohEndpoint? =
    when (dns.provider) {
        VocarooDnsProvider.SYSTEM -> null
        VocarooDnsProvider.COMSS -> preset(COMSS_URL, COMSS_IPS)
        VocarooDnsProvider.XBOX -> preset(XBOX_URL, XBOX_IPS)
        VocarooDnsProvider.GEOHIDE -> preset(GEOHIDE_URL, GEOHIDE_IPS)
        VocarooDnsProvider.CUSTOM -> customDohUrl(dns.customUrl)?.let { VocarooDohEndpoint(it, emptyList()) }
    }

/** Свой адрес принимается только по https и без логина в адресе. */
internal fun customDohUrl(value: String): HttpUrl? =
    value
        .trim()
        .toHttpUrlOrNull()
        ?.takeIf { it.isHttps && it.username.isEmpty() && it.password.isEmpty() }

private fun preset(
    url: String,
    ips: List<String>,
): VocarooDohEndpoint =
    VocarooDohEndpoint(
        url = requireNotNull(url.toHttpUrlOrNull()),
        bootstrap = ips.map { InetAddress.getByName(it) },
    )

private const val COMSS_URL = "https://dns.comss.one/dns-query"
private val COMSS_IPS = listOf("195.133.25.16", "212.109.195.93", "83.220.169.155")
private const val XBOX_URL = "https://xbox-dns.ru/dns-query"
private val XBOX_IPS = listOf("111.88.96.54", "111.88.96.55")
private const val GEOHIDE_URL = "https://dns.geohide.ru/dns-query"
private val GEOHIDE_IPS = listOf("193.233.112.67", "193.233.112.68", "193.233.112.88", "37.230.192.51")
