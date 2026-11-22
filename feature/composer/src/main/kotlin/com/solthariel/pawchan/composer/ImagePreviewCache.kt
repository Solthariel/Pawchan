package com.solthariel.pawchan.composer

import com.solthariel.pawchan.domain.drafts.DraftImageEdit

/** Main-thread/session confined. Eviction drops ownership, never recycles a displayed frame. */
internal class ImagePreviewCache<T> {
    private val frames = LinkedHashMap<DraftImageEdit, T>()

    operator fun get(edit: DraftImageEdit): T? = frames[edit]

    fun put(
        edit: DraftImageEdit,
        frame: T,
    ) {
        frames.remove(edit)
        frames[edit] = frame
        while (frames.size > MAX_PREVIEW_FRAMES) frames.remove(frames.keys.first())
    }
}

private const val MAX_PREVIEW_FRAMES = 4
