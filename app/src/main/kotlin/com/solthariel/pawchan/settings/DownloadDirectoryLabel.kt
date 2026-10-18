package com.solthariel.pawchan.settings

import java.net.URLDecoder
import java.nio.charset.StandardCharsets

internal fun downloadDirectoryLabel(uri: String?): String {
    if (uri == null) return "Выбрать папку"
    val encoded = uri.substringAfter("/tree/", "").substringBefore("/document/")
    val decoded =
        try {
            URLDecoder.decode(encoded.replace("+", "%2B"), StandardCharsets.UTF_8.name())
        } catch (
            _: IllegalArgumentException,
        ) {
            ""
        }
    val path = decoded.substringAfter(':', decoded)
    return path.ifBlank { "Выбранная папка" }
}
