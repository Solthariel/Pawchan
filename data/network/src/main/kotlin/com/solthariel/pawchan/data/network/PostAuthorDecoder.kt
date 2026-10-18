package com.solthariel.pawchan.data.network

import com.solthariel.pawchan.domain.model.PostBadge
import com.solthariel.pawchan.domain.model.RemoteUrl
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.intOrNull
import okhttp3.HttpUrl.Companion.toHttpUrl
import org.jsoup.Jsoup
import org.jsoup.nodes.Element

internal fun decodePostAuthor(html: String?): String? {
    val body = html?.let { Jsoup.parseBodyFragment(it).body() }
    return body?.let(::decodePostAuthor)
}

internal fun decodePostAuthor(body: Element): String? {
    val identity = body.selectFirst("span[id^=id_tag_]")
    return (identity?.text() ?: body.text()).replace('\u00a0', ' ').trim().takeIf(String::isNotBlank)
}

internal fun decodeAuthorColor(html: String?): Int? {
    val style = html?.let { Jsoup.parseBodyFragment(it).selectFirst("span[style]")?.attr("style") } ?: return null
    val hex = AUTHOR_HEX.find(style)?.groupValues?.get(1)
    val parts =
        AUTHOR_RGB
            .find(style)
            ?.groupValues
            ?.drop(1)
            ?.mapNotNull(String::toIntOrNull)
    return when {
        hex != null -> {
            hex.toLongOrNull(HEX_RADIX)?.toInt()?.or(AUTHOR_ALPHA)
        }

        parts != null && parts.size == RGB_COMPONENTS && parts.all { it in 0..COLOR_MAX } -> {
            AUTHOR_ALPHA or (parts[0] shl RED_SHIFT) or (parts[1] shl GREEN_SHIFT) or parts[2]
        }

        else -> {
            null
        }
    }
}

internal fun decodePostBadges(html: String?): List<PostBadge> =
    html
        ?.let {
            val base = "$DVACH_ORIGIN/".toHttpUrl()
            Jsoup.parseBodyFragment(it).select("img[src]").mapNotNull { img ->
                val src = img.attr("src")
                val resolved =
                    base.resolve(src)?.takeIf { address ->
                        address.scheme == "https" &&
                            address.host == base.host
                    }
                val url = resolved?.toString()?.let(RemoteUrl::parse)
                url?.let { address ->
                    val label = img.attr("title").ifBlank { img.attr("alt") }
                    PostBadge(label, address, countryBadgeSymbol(src, label))
                }
            }
        }.orEmpty()

private val AUTHOR_HEX = Regex("(?:^|;)\\s*color\\s*:\\s*#([0-9a-fA-F]{6})(?:;|$)")
private val AUTHOR_RGB = Regex("(?:^|;)\\s*color\\s*:\\s*rgb\\(\\s*(\\d+)\\s*,\\s*(\\d+)\\s*,\\s*(\\d+)\\s*\\)(?:;|$)")
private const val HEX_RADIX = 16
private const val AUTHOR_ALPHA = -0x1000000
private const val RGB_COMPONENTS = 3
private const val COLOR_MAX = 255
private const val RED_SHIFT = 16
private const val GREEN_SHIFT = 8

internal fun decodeOriginalPoster(value: JsonElement?): Boolean {
    val primitive = value as? JsonPrimitive ?: return false
    return primitive.booleanOrNull ?: (primitive.intOrNull?.let { it > 0 } ?: false)
}
