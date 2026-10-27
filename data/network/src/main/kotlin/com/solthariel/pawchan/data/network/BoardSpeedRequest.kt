package com.solthariel.pawchan.data.network

import com.solthariel.pawchan.domain.model.BoardId
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.intOrNull
import okhttp3.CacheControl
import okhttp3.HttpUrl
import okhttp3.Request

internal object BoardSpeedRequest {
    fun create(board: BoardId): Request =
        Request
            .Builder()
            .url(
                HttpUrl
                    .Builder()
                    .scheme("https")
                    .host(DVACH_HOST)
                    .addPathSegment(board.value)
                    .addPathSegment("index.json")
                    .build(),
            ).header("Accept", JSON_MEDIA_TYPE)
            .header("User-Agent", USER_AGENT)
            .cacheControl(CacheControl.FORCE_NETWORK)
            .get()
            .build()
}

internal fun decodeBoardSpeed(payload: String): Int? =
    ((dvachReadJson().parseToJsonElement(payload) as? JsonObject)?.get("board_speed") as? JsonPrimitive)
        ?.intOrNull
        ?.takeIf { it >= 0 }
