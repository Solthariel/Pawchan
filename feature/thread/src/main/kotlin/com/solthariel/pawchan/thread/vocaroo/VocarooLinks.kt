package com.solthariel.pawchan.thread.vocaroo

import java.net.URI

/** Id записи из ссылки vocaroo.com/<id> или voca.ro/<id>; null — не запись Vocaroo. */
internal fun vocarooId(url: String): String? {
    val uri = runCatching { URI(url.trim()) }.getOrNull()
    val host = uri?.host?.lowercase()?.removePrefix("www.")
    val supported = uri?.scheme?.lowercase() in setOf("http", "https") && host in VOCAROO_HOSTS
    val segments =
        uri
            ?.path
            .orEmpty()
            .split('/')
            .filter(String::isNotEmpty)
    val id =
        when {
            !supported -> null
            segments.size == 1 -> segments[0]
            segments.size == 2 && segments[0] == "embed" -> segments[1]
            else -> null
        }
    return id?.takeIf { VOCAROO_ID.matches(it) && it.lowercase() !in RESERVED_PATHS }
}

/** Прямой mp3: основной сервер отдаёт файл без Referer. */
internal fun vocarooMp3Url(id: String): String = "https://media.vocaroo.com/mp3/$id"

internal fun formatVocarooTime(millis: Int): String {
    val seconds = (millis / MILLIS_PER_SECOND).coerceAtLeast(0)
    return "${seconds / SECONDS_PER_MINUTE}:${(seconds % SECONDS_PER_MINUTE).toString().padStart(2, '0')}"
}

private val VOCAROO_HOSTS = setOf("vocaroo.com", "voca.ro")
private val VOCAROO_ID = Regex("[A-Za-z0-9]{6,24}")
private val RESERVED_PATHS = setOf("upload", "settings", "about", "privacy", "terms", "faq", "apps")
private const val MILLIS_PER_SECOND = 1000
private const val SECONDS_PER_MINUTE = 60
