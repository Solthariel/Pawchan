package com.solthariel.pawchan.core.ui

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance

/** Semantic forum colors: never recolor links to match the chosen accent. */
data class PostTextColors(
    val link: Color,
    val quote: Color,
    val red: Color,
)

fun postTextColors(surface: Color): PostTextColors =
    if (surface.luminance() < DARK_POST_SURFACE) DARK_POST_TEXT_COLORS else LIGHT_POST_TEXT_COLORS

val LIGHT_POST_TEXT_COLORS =
    PostTextColors(Color(LIGHT_LINK), Color(LIGHT_QUOTE), Color(LIGHT_RED))
val DARK_POST_TEXT_COLORS =
    PostTextColors(Color(DARK_LINK), Color(DARK_QUOTE), Color(DARK_RED))

private const val DARK_POST_SURFACE = 0.5f
private const val LIGHT_LINK = 0xFF2457A6
private const val LIGHT_QUOTE = 0xFF326B29
private const val LIGHT_RED = 0xFFB3261E
private const val DARK_LINK = 0xFFA8C7FA
private const val DARK_QUOTE = 0xFFA5D395
private const val DARK_RED = 0xFFFFB4AB
