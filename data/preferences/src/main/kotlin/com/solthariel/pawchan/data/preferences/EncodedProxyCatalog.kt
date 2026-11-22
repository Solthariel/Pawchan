package com.solthariel.pawchan.data.preferences

internal data class EncodedProxyCatalog(
    val catalog: String,
    val secrets: ByteArray?,
)
