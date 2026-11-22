package com.solthariel.pawchan.ui

import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import com.solthariel.pawchan.R
import com.solthariel.pawchan.domain.settings.AppFont
import com.solthariel.pawchan.domain.settings.TypographyPreferences

@Composable
internal fun appTypography(value: TypographyPreferences): Typography = remember(value) { buildAppTypography(value) }

internal fun buildAppTypography(preferences: TypographyPreferences): Typography {
    val default = Typography()
    return if (preferences == TypographyPreferences()) default else default.customize(preferences)
}

private fun Typography.customize(preferences: TypographyPreferences): Typography {
    val family = fontFamily(preferences.font)

    fun TextStyle.customized(): TextStyle =
        copy(
            fontFamily = family ?: fontFamily,
            fontWeight =
                fontWeight?.let {
                    FontWeight((it.weight + preferences.emphasis.adjustment).coerceIn(MIN_WEIGHT, MAX_WEIGHT))
                },
            lineHeight = lineHeight * (preferences.leading.percent / PERCENT_DIVISOR),
        )
    return copy(
        displayLarge = displayLarge.customized(),
        displayMedium = displayMedium.customized(),
        displaySmall = displaySmall.customized(),
        headlineLarge = headlineLarge.customized(),
        headlineMedium = headlineMedium.customized(),
        headlineSmall = headlineSmall.customized(),
        titleLarge = titleLarge.customized(),
        titleMedium = titleMedium.customized(),
        titleSmall = titleSmall.customized(),
        bodyLarge = bodyLarge.customized(),
        bodyMedium = bodyMedium.customized(),
        bodySmall = bodySmall.customized(),
        labelLarge = labelLarge.customized(),
        labelMedium = labelMedium.customized(),
        labelSmall = labelSmall.customized(),
    )
}

@OptIn(ExperimentalTextApi::class)
private fun fontFamily(font: AppFont): FontFamily? {
    val resource =
        when (font) {
            AppFont.SYSTEM -> return null
            AppFont.ROBOTO_FLEX -> R.font.robotoflex
            AppFont.ROBOTO_SERIF -> R.font.robotoserif
            AppFont.ROBOTO_MONO -> R.font.robotomono
        }
    val maxAxis = if (font == AppFont.ROBOTO_MONO) MAX_MONO_WEIGHT else MAX_WEIGHT
    return FontFamily(
        (MIN_WEIGHT..MAX_WEIGHT step WEIGHT_STEP).map { weight ->
            Font(
                resId = resource,
                weight = FontWeight(weight),
                variationSettings = FontVariation.Settings(FontVariation.weight(weight.coerceAtMost(maxAxis))),
            )
        },
    )
}

private const val MIN_WEIGHT = 100
private const val MAX_WEIGHT = 900
private const val MAX_MONO_WEIGHT = 700
private const val WEIGHT_STEP = 100
private const val PERCENT_DIVISOR = 100f
