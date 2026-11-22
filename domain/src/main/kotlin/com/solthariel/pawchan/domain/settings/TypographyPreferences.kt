package com.solthariel.pawchan.domain.settings

/** Application-wide text choices. Defaults preserve the system typeface and existing type scale. */
enum class AppFont { SYSTEM, ROBOTO_FLEX, ROBOTO_SERIF, ROBOTO_MONO }

enum class FontEmphasis(
    val adjustment: Int,
) {
    LIGHT(LIGHT_WEIGHT_ADJUSTMENT),
    NORMAL(0),
    STRONG(STRONG_WEIGHT_ADJUSTMENT),
}

enum class TextLeading(
    val percent: Int,
) {
    STANDARD(STANDARD_LEADING_PERCENT),
    RELAXED(RELAXED_LEADING_PERCENT),
}

data class TypographyPreferences(
    val font: AppFont = AppFont.SYSTEM,
    val emphasis: FontEmphasis = FontEmphasis.NORMAL,
    val leading: TextLeading = TextLeading.STANDARD,
)

private const val LIGHT_WEIGHT_ADJUSTMENT = -100
private const val STRONG_WEIGHT_ADJUSTMENT = 100
private const val STANDARD_LEADING_PERCENT = 100
private const val RELAXED_LEADING_PERCENT = 115
