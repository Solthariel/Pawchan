package com.solthariel.pawchan.composer

import com.solthariel.pawchan.domain.posting.EmojiCaptchaOption

/** Pure presentation transform. The server number belongs to this response, never to a slot. */
internal fun stableCaptchaKeyboard(
    previous: List<EmojiCaptchaOption?>,
    incoming: List<EmojiCaptchaOption>,
): List<EmojiCaptchaOption?> {
    val remaining =
        previous
            .withIndex()
            .filter { it.value != null }
            .groupBy { CaptchaImageKey(requireNotNull(it.value).image) }
            .mapValues { ArrayDeque(it.value) }
    val slots = MutableList<EmojiCaptchaOption?>(maxOf(previous.size, incoming.size)) { null }
    val additions = mutableListOf<EmojiCaptchaOption>()
    for (option in incoming) {
        val old = remaining[CaptchaImageKey(option.image)]?.removeFirstOrNull()
        if (old == null) {
            additions += option
        } else {
            slots[old.index] = option.copy(image = requireNotNull(old.value).image)
        }
    }
    var free = 0
    for (option in additions) {
        while (slots[free] != null) free++
        slots[free] = option
    }
    return slots.toList()
}

/** Hashing only accelerates lookup: collisions are always checked against the actual bytes. */
private class CaptchaImageKey(
    private val bytes: ByteArray,
) {
    private val hash = bytes.contentHashCode()

    override fun hashCode(): Int = hash

    override fun equals(other: Any?): Boolean = other is CaptchaImageKey && bytes.contentEquals(other.bytes)
}
