package com.solthariel.pawchan.settings

internal fun parseSeedHex(text: String): Int? {
    val value = text.trim().removePrefix("#")
    return value
        .takeIf { it.length == HEX_RGB_LENGTH && it.all { digit -> digit in "0123456789abcdefABCDEF" } }
        ?.toIntOrNull(HEX_RADIX)
}

internal fun seedHex(seed: Int): String = "#" + seed.toString(HEX_RADIX).padStart(HEX_RGB_LENGTH, '0').uppercase()

private const val HEX_RGB_LENGTH = 6
private const val HEX_RADIX = 16
