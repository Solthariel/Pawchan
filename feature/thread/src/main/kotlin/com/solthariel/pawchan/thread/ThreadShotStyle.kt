package com.solthariel.pawchan.thread

data class ThreadShotStyle(
    val textSizeSp: Int = DEFAULT_THREAD_TEXT_SIZE,
    val background: Int = 0xFF121212.toInt(),
    val surface: Int = 0xFF1E1E1E.toInt(),
    val border: Int = 0xFF49454F.toInt(),
    val text: Int = 0xFFE6E1E5.toInt(),
    val secondaryText: Int = 0xFFCAC4D0.toInt(),
    val accent: Int = 0xFFD0BCFF.toInt(),
)

internal const val DEFAULT_THREAD_TEXT_SIZE = 12
