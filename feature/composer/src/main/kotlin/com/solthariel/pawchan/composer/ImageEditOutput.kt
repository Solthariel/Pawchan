package com.solthariel.pawchan.composer

internal data class ImageEditOutput(
    val content: ByteArray,
    val extension: String,
    val mime: String,
)
