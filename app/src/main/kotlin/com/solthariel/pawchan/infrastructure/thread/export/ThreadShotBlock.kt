package com.solthariel.pawchan.infrastructure.thread.export

import android.text.StaticLayout
import com.solthariel.pawchan.thread.ThreadExportPost

internal data class ThreadShotBlock(
    val post: ThreadExportPost,
    val header: StaticLayout,
    val body: StaticLayout,
    val mediaHeight: Int,
    val subject: StaticLayout?,
    val cardHeight: Int,
) {
    val height: Int get() = cardHeight + SHOT_CARD_GAP
}
