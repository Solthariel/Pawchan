package com.solthariel.pawchan.data.network

import java.util.Locale

internal fun countryBadgeSymbol(
    source: String,
    label: String,
): String? {
    if (!source.contains("flag", ignoreCase = true)) return null
    val file = source.substringAfterLast('/').substringBeforeLast('.').lowercase(Locale.ROOT)
    return COUNTRY_SYMBOLS[file] ?: COUNTRY_SYMBOLS[label.lowercase(Locale.ROOT)]
}

private val COUNTRY_SYMBOLS: Map<String, String> =
    buildMap {
        for (code in Locale.getISOCountries()) {
            val locale = Locale.Builder().setRegion(code).build()
            val symbol =
                String(Character.toChars(REGIONAL_BASE + code[0].code - 'A'.code)) +
                    String(Character.toChars(REGIONAL_BASE + code[1].code - 'A'.code))
            put(code.lowercase(Locale.ROOT), symbol)
            put(locale.getDisplayCountry(Locale.ENGLISH).lowercase(Locale.ROOT), symbol)
            put(locale.getDisplayCountry(Locale.forLanguageTag("ru")).lowercase(Locale.ROOT), symbol)
        }
    }

private const val REGIONAL_BASE = 0x1F1E6
