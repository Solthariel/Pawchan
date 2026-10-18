package com.solthariel.pawchan.data.network

import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.decodeFromJsonElement

internal inline fun <reified T> Json.decodeElementOrNull(element: JsonElement): T? =
    try {
        decodeFromJsonElement<T>(element)
    } catch (_: SerializationException) {
        null
    }
