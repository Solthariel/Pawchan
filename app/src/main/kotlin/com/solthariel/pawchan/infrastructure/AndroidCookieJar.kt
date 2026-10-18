package com.solthariel.pawchan.infrastructure

import android.webkit.CookieManager
import com.solthariel.pawchan.domain.boards.PostingAccessRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import okhttp3.Cookie
import okhttp3.CookieJar
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrl

internal class AndroidCookieJar(
    private val cookieManager: CookieManager = CookieManager.getInstance(),
) : CookieJar,
    PostingAccessRepository {
    @Volatile private var origin = DVACH_ORIGIN
    private val mutablePostingAccess = MutableStateFlow(hasPostingCookie())
    override val hasPostingAccess: StateFlow<Boolean> = mutablePostingAccess.asStateFlow()

    init {
        cookieManager.setAcceptCookie(true)
    }

    fun selectOrigin(host: String) {
        origin = "https://$host/".toHttpUrl()
        mutablePostingAccess.value = hasPostingCookie()
    }

    override fun saveFromResponse(
        url: HttpUrl,
        cookies: List<Cookie>,
    ) {
        cookies.forEach { cookie ->
            cookieManager.setCookie(url.toString(), cookie.toString())
        }
        cookieManager.flush()
        mutablePostingAccess.value = hasPostingCookie()
    }

    override fun loadForRequest(url: HttpUrl): List<Cookie> {
        val header = cookieManager.getCookie(url.toString())
        return parseCookieHeader(header, url)
    }

    private fun hasPostingCookie(): Boolean =
        parseCookieHeader(
            cookieManager.getCookie(origin.toString()),
            origin,
        ).any { cookie -> cookie.name == USERCODE_AUTH_COOKIE && cookie.value.isNotBlank() }
}

internal fun parseCookieHeader(
    header: String?,
    url: HttpUrl,
): List<Cookie> =
    header
        ?.split(';')
        ?.mapNotNull { rawCookie -> rawCookie.toCookie(url) }
        .orEmpty()

private fun String.toCookie(url: HttpUrl): Cookie? {
    val separator = indexOf('=')
    if (separator <= 0) {
        return null
    }
    return try {
        val builder =
            Cookie
                .Builder()
                .name(substring(0, separator).trim())
                .value(substring(separator + 1).trim())
                .hostOnlyDomain(url.host)
                .path("/")
        if (url.isHttps) {
            builder.secure()
        }
        builder.build()
    } catch (_: IllegalArgumentException) {
        null
    }
}

private val DVACH_ORIGIN = "https://2ch.su/".toHttpUrl()
private const val USERCODE_AUTH_COOKIE = "usercode_auth"
