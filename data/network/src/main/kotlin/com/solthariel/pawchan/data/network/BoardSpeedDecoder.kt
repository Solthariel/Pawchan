package com.solthariel.pawchan.data.network

import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.intOrNull

internal fun CatalogDto.extractBoardSpeed(): Int =
    sequenceOf(
        boardSpeed,
        speed,
        board.boardSpeed,
        board.speed,
    ).mapNotNull { value -> value.toBoardSpeed() }
        .firstOrNull { it > 0 }
        ?: 0

private fun JsonElement?.toBoardSpeed(): Int? =
    (this as? JsonPrimitive)
        ?.let { primitive -> primitive.intOrNull ?: primitive.content.toIntOrNull() }
        ?.coerceAtLeast(0)
