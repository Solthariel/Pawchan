package com.solthariel.pawchan.data.network

import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.intOrNull

internal fun JsonObject.flag(key: String): Boolean? {
    val value = get(key) as? JsonPrimitive
    return value?.booleanOrNull ?: when (value?.intOrNull) {
        0 -> false
        1 -> true
        else -> null
    }
}
