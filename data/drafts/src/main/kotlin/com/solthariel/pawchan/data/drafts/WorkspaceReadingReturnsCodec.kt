package com.solthariel.pawchan.data.drafts

import com.solthariel.pawchan.domain.model.ThreadAddress
import com.solthariel.pawchan.domain.workspace.ReadingReturnPoint
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.long
import kotlinx.serialization.json.put

internal fun encodeReadingReturns(points: Map<ThreadAddress, ReadingReturnPoint>): JsonArray =
    JsonArray(
        points.map { (address, point) ->
            buildJsonObject {
                put("address", address.encodeAddress())
                put("post", point.postId)
                put("offset", point.offset)
            }
        },
    )

internal fun decodeReadingReturns(element: JsonElement?): Map<ThreadAddress, ReadingReturnPoint> =
    element
        ?.jsonArray
        ?.associate { item ->
            val value = item.jsonObject
            value.requiredValue("address").jsonObject.decodeAddress() to
                ReadingReturnPoint(
                    value.requiredValue("post").jsonPrimitive.long,
                    value.requiredValue("offset").jsonPrimitive.int,
                )
        }.orEmpty()
