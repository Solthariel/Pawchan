package com.solthariel.pawchan.catalog

internal fun List<CatalogThreadUi>.matching(query: String): List<CatalogThreadUi> {
    val normalized = query.trim()
    return if (normalized.isBlank()) {
        this
    } else {
        filter { thread ->
            thread.subject?.contains(normalized, ignoreCase = true) == true ||
                thread.comment.contains(normalized, ignoreCase = true)
        }
    }
}
