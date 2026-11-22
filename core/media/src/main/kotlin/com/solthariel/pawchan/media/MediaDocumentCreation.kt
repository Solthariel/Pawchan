package com.solthariel.pawchan.media

import java.io.FileNotFoundException
import java.util.Locale

/** Never hide revoked folder access: the caller can ask for a new directory. */
internal fun <A, U> createMediaDocuments(
    items: List<A>,
    create: (A) -> U?,
    discard: (U) -> Unit,
): List<Pair<A, U>> {
    val created = mutableListOf<Pair<A, U>>()
    try {
        items.forEach { item ->
            try {
                create(item)?.let { created.add(item to it) }
            } catch (_: FileNotFoundException) {
                // Other attachments may still be saved.
            }
        }
    } catch (failure: SecurityException) {
        created.forEach { (_, uri) -> discard(uri) }
        throw failure
    }
    return created
}

internal fun mediaMimeTypeForName(
    name: String,
    lookup: (String) -> String?,
): String {
    val extension = name.substringAfterLast('.', "").lowercase(Locale.ROOT)
    return extension.takeIf { it.isNotEmpty() }?.let(lookup) ?: "application/octet-stream"
}
