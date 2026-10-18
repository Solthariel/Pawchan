package com.solthariel.pawchan.data.network

import com.solthariel.pawchan.domain.posting.PostingIcon
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.intOrNull

internal fun decodePostingIcons(metadata: JsonObject?): List<PostingIcon> {
    if (metadata?.flag("enable_icons") != true) return emptyList()
    return (metadata["icons"] as? JsonArray)
        ?.mapNotNull { value ->
            val item = value as? JsonObject ?: return@mapNotNull null
            val id = (item["num"] as? JsonPrimitive)?.intOrNull ?: return@mapNotNull null
            val name = (item["name"] as? JsonPrimitive)?.contentOrNull ?: return@mapNotNull null
            val imageUrl =
                (item["url"] as? JsonPrimitive)
                    ?.contentOrNull
                    ?.toPostingIconUrl()
            if (id > 0 && name.isNotBlank()) PostingIcon(id, name, imageUrl) else null
        }?.distinctBy { it.id }
        .orEmpty()
}

private fun String.toPostingIconUrl(): String? =
    when {
        startsWith("https://") -> this
        startsWith("/") -> "https://$DVACH_HOST$this"
        else -> null
    }
