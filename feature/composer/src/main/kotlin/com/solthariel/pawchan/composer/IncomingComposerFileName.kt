package com.solthariel.pawchan.composer

import com.solthariel.pawchan.data.network.PostingMediaBytes

/** Some IMEs expose bytes without a filename extension. Identify bytes, never trust a MIME label. */
internal fun incomingComposerFileName(
    name: String,
    bytes: ByteArray,
): String {
    if (name.substringAfterLast('.', "").isNotEmpty()) return name
    val format = PostingMediaBytes.format(bytes)
    return if (format == null) name else "$name.$format"
}
