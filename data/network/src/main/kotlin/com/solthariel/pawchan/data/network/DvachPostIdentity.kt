package com.solthariel.pawchan.data.network

import org.jsoup.Jsoup

/** Makaba wire identity; never infer this marker from arbitrary UI text or another site. */
internal data class DvachPostIdentity(
    val name: String?,
    val sage: Boolean,
)

internal fun decodeDvachPostIdentity(
    nameHtml: String?,
    email: String?,
): DvachPostIdentity {
    val body = nameHtml?.let { Jsoup.parseBodyFragment(it).body() }
    val plain = body?.text()?.replace('\u00a0', ' ')?.trim()
    val marker = plain?.let(HEAVEN_ID::matchEntire)
    val name =
        if (marker != null) {
            marker.groupValues[1].trim().takeIf(String::isNotBlank)
        } else {
            body?.let(::decodePostAuthor)
        }
    val address = email?.trim()
    val sage = address.equals("sage", ignoreCase = true) || address.equals("mailto:sage", ignoreCase = true)
    return DvachPostIdentity(name, sage || marker != null)
}

private val HEAVEN_ID = Regex("^(.+?)\\s+ID:\\s+Heaven$")
