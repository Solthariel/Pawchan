package com.solthariel.pawchan.data.network

import okhttp3.Cookie
import okhttp3.CookieJar
import okhttp3.HttpUrl

/** Cookie только в памяти: сессия капчи Архивача живёт, пока открыто приложение. */
class ArhivachCookieJar : CookieJar {
    private val cookies = mutableMapOf<String, Cookie>()

    @Synchronized
    override fun saveFromResponse(
        url: HttpUrl,
        cookies: List<Cookie>,
    ) {
        cookies.forEach { this.cookies["${it.domain}|${it.path}|${it.name}"] = it }
    }

    @Synchronized
    override fun loadForRequest(url: HttpUrl): List<Cookie> {
        val now = System.currentTimeMillis()
        cookies.values.removeAll { it.expiresAt < now }
        return cookies.values.filter { it.matches(url) }
    }
}
