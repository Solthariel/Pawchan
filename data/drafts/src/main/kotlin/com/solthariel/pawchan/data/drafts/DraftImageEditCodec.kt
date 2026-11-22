package com.solthariel.pawchan.data.drafts

import com.solthariel.pawchan.domain.drafts.DraftCrop
import com.solthariel.pawchan.domain.drafts.DraftImageEdit
import com.solthariel.pawchan.domain.drafts.DraftPoint
import com.solthariel.pawchan.domain.drafts.MAX_IMAGE_MARKS
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.floatOrNull
import kotlinx.serialization.json.put

internal fun encodeImageEdit(edit: DraftImageEdit): JsonObject =
    buildJsonObject {
        put("version", EDIT_VERSION)
        val crop = listOf(edit.crop.left, edit.crop.top, edit.crop.right, edit.crop.bottom)
        put("crop", JsonArray(crop.map(::JsonPrimitive)))
        put("turns", edit.quarterTurns)
        put("mirror", edit.mirrored)
        put("marks", JsonArray(edit.marks.map(::encodeImageMark)))
    }

internal fun decodeImageEdit(root: JsonObject): DraftImageEdit {
    require(root.integer("version") == EDIT_VERSION)
    val crop = root.array("crop")
    require(crop.size == CROP_COMPONENTS)
    val values = crop.map { it.floatValue() }
    val marks = root.array("marks")
    require(marks.size <= MAX_IMAGE_MARKS)
    return DraftImageEdit(
        crop = DraftCrop(values[0], values[1], values[RIGHT_INDEX], values[BOTTOM_INDEX]),
        quarterTurns = root.integer("turns"),
        mirrored = root.requiredBoolean("mirror"),
        marks = marks.map { decodeImageMark(it as? JsonObject ?: error("Invalid image mark")) },
    )
}

internal fun encodePoint(point: DraftPoint): JsonArray {
    val x = JsonPrimitive(point.x)
    val y = JsonPrimitive(point.y)
    return JsonArray(listOf(x, y))
}

internal fun decodePoint(element: JsonElement): DraftPoint {
    val value = element as? JsonArray ?: error("Invalid image point")
    require(value.size == 2)
    return DraftPoint(value[0].floatValue(), value[1].floatValue())
}

internal fun JsonObject.array(key: String): JsonArray = get(key) as? JsonArray ?: error("Invalid image array")

internal fun JsonElement.floatValue(): Float = requireNotNull((this as? JsonPrimitive)?.floatOrNull)

internal fun JsonObject.float(key: String): Float = requireNotNull(get(key)).floatValue()

internal fun JsonObject.requiredBoolean(key: String): Boolean {
    val primitive = get(key) as? JsonPrimitive
    return requireNotNull(primitive?.booleanOrNull)
}

private const val EDIT_VERSION = 1
private const val CROP_COMPONENTS = 4

private const val RIGHT_INDEX = 2
private const val BOTTOM_INDEX = 3
