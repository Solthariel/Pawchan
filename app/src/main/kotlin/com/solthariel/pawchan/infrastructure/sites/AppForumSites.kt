package com.solthariel.pawchan.infrastructure.sites

import androidx.compose.runtime.staticCompositionLocalOf

internal val LocalForumMirror = staticCompositionLocalOf { "2ch.su" }

/** All adapters, with the selected origin scoped to the application surface. */
internal fun appForumWebLinks(mirror: String = "2ch.su"): List<ForumWebLinks> = listOf(DvachWebLinks(mirror))
