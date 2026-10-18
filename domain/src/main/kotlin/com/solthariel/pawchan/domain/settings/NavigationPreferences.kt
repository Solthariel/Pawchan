package com.solthariel.pawchan.domain.settings

/** Presentation limits never remove the underlying threads. */
data class NavigationPreferences(
    val pages: Int = 5,
    val favorites: Int = 5,
    val replies: Int = 5,
) {
    fun normalized(): NavigationPreferences =
        copy(
            pages = pages.coerceIn(MIN_VISIBLE, MAX_VISIBLE),
            favorites = favorites.coerceIn(MIN_VISIBLE, MAX_VISIBLE),
            replies = replies.coerceIn(MIN_VISIBLE, MAX_VISIBLE),
        )
}

fun validMirrorHost(value: String): Boolean =
    value.length in MIN_HOST_LENGTH..MAX_HOST_LENGTH && value == value.lowercase() && value.split('.').size >= 2 &&
        value.split('.').all { part ->
            part.isNotEmpty() && part.length <= MAX_LABEL_LENGTH && part.first().isLetterOrDigit() &&
                part.last().isLetterOrDigit() && part.all { it in 'a'..'z' || it in '0'..'9' || it == '-' }
        }

const val MIN_VISIBLE = 1
const val MAX_VISIBLE = 20
private const val MIN_HOST_LENGTH = 4
private const val MAX_HOST_LENGTH = 253
private const val MAX_LABEL_LENGTH = 63
