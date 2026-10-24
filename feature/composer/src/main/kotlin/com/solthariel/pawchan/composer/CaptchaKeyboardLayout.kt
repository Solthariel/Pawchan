package com.solthariel.pawchan.composer

import com.solthariel.pawchan.domain.posting.EmojiCaptchaOption

internal fun stableCaptchaKeyboard(
    previous: List<EmojiCaptchaOption?>,
    incoming: List<EmojiCaptchaOption>,
): List<EmojiCaptchaOption?> {
    val existing = previous.mapIndexedNotNull { index, option -> option?.let { IndexedValue(index, it) } }
    val orderedPrevious = existing.sortedWith(::compareCaptchaOptions)
    val orderedIncoming = incoming.withIndex().sortedWith(::compareCaptchaOptions)
    val result = MutableList<EmojiCaptchaOption?>(maxOf(previous.size, incoming.size)) { null }
    val assigned = BooleanArray(incoming.size)
    var oldPosition = 0
    var newPosition = 0
    while (oldPosition < orderedPrevious.size && newPosition < orderedIncoming.size) {
        val old = orderedPrevious[oldPosition]
        val fresh = orderedIncoming[newPosition]
        val comparison = compareCaptchaBytes(old.value.image, fresh.value.image)
        when {
            comparison < 0 -> {
                oldPosition++
            }

            comparison > 0 -> {
                newPosition++
            }

            else -> {
                result[old.index] = fresh.value.copy(image = old.value.image)
                assigned[fresh.index] = true
                oldPosition++
                newPosition++
            }
        }
    }
    var vacancy = 0
    incoming.forEachIndexed { index, option ->
        if (!assigned[index]) {
            while (result[vacancy] != null) vacancy++
            result[vacancy] = option
        }
    }
    return result.toList()
}

private fun compareCaptchaOptions(
    left: IndexedValue<EmojiCaptchaOption>,
    right: IndexedValue<EmojiCaptchaOption>,
): Int {
    val imageOrder = compareCaptchaBytes(left.value.image, right.value.image)
    return if (imageOrder == 0) left.index.compareTo(right.index) else imageOrder
}

private fun compareCaptchaBytes(
    left: ByteArray,
    right: ByteArray,
): Int {
    for (index in 0 until minOf(left.size, right.size)) {
        val difference = left[index].compareTo(right[index])
        if (difference != 0) return difference
    }
    return left.size.compareTo(right.size)
}
