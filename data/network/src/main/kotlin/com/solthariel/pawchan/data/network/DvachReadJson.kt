package com.solthariel.pawchan.data.network

import kotlinx.serialization.json.Json

/** Shared strict read-API configuration, not posting/sticker protocol configuration. */
internal fun dvachReadJson(): Json =
    Json {
        ignoreUnknownKeys = true
        explicitNulls = false
        isLenient = false
    }
