package com.solthariel.pawchan.infrastructure

import android.webkit.CookieManager

/** Куки одного сайта (2ch на текущем зеркале): имя и значение. */
internal interface SiteCookies {
    val host: String

    fun list(): List<Pair<String, String>>

    fun delete(name: String)
}

internal class WebViewSiteCookies(
    override val host: String,
    private val manager: CookieManager = CookieManager.getInstance(),
) : SiteCookies {
    override fun list(): List<Pair<String, String>> = parseCookiePairs(manager.getCookie("https://$host/"))

    /** Срок в прошлом и для куки только этого хоста, и для куки домена: какая из них была, та и удалится. */
    override fun delete(name: String) {
        val url = "https://$host/"
        expiredCookieHeaders(name, host).forEach { header -> manager.setCookie(url, header) }
        manager.flush()
    }
}

internal fun parseCookiePairs(header: String?): List<Pair<String, String>> =
    header
        ?.split(';')
        ?.mapNotNull { raw ->
            val separator = raw.indexOf('=')
            if (separator <= 0) null else raw.substring(0, separator).trim() to raw.substring(separator + 1).trim()
        }?.filter { it.first.isNotEmpty() }
        .orEmpty()

internal fun expiredCookieHeaders(
    name: String,
    host: String,
): List<String> {
    val expired = "$name=; Max-Age=0; Expires=Thu, 01 Jan 1970 00:00:00 GMT; Path=/"
    return listOf(expired, "$expired; Domain=$host", "$expired; Domain=.$host")
}

/** «Удалять при выходе»: и при закрытии, и при следующем запуске, если процесс убили. */
internal fun SiteCookies.deleteOnExit(rules: CookieRules) {
    val present = list().map { it.first }.toSet()
    rules.deleteOnExit.filter { it in present }.forEach(::delete)
}
