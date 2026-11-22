package com.solthariel.pawchan.data.network

import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import org.jsoup.nodes.Element

internal fun Element.archiveOffset(url: String): Int? =
    absUrl("href")
        .toHttpUrlOrNull()
        ?.takeIf { parsed ->
            parsed.host == "arhivach.vc" &&
                parsed.queryParameter("tags") == url.toHttpUrlOrNull()?.queryParameter("tags")
        }?.encodedPath
        ?.let { path ->
            ARCHIVE_PAGE_PATH
                .matchEntire(path)
                ?.groupValues
                ?.get(1)
                ?.toIntOrNull()
        }?.takeIf { it % ARCHIVE_PAGE_SIZE == 0 }

private val ARCHIVE_PAGE_PATH = Regex("^/index/(\\d+)/?$")
