package com.solthariel.pawchan.thread

import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.staticCompositionLocalOf

/**
 * Раскрытые спойлеры треда по номеру поста. Живут, пока открыт тред: не сбрасываются
 * ни обновлением (посты пересобираются), ни прокруткой поста за экран.
 */
internal class RevealedSpoilers {
    private val revealed = mutableStateMapOf<Long, Set<Int>>()

    operator fun get(post: Long): Set<Int> = revealed[post].orEmpty()

    fun reveal(
        post: Long,
        spoiler: Int,
    ) {
        revealed[post] = get(post) + spoiler
    }
}

internal val LocalRevealedSpoilers = staticCompositionLocalOf<RevealedSpoilers?> { null }
