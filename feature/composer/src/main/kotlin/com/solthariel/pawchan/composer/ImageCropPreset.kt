package com.solthariel.pawchan.composer

import com.solthariel.pawchan.domain.drafts.DraftImageEdit
import com.solthariel.pawchan.domain.drafts.originalOutputAspect

internal enum class ImageCropPreset(
    val label: String,
) {
    FREE("Свободно"),
    ORIGINAL("Исходное"),
    SQUARE("1:1"),
    FOUR_THREE("4:3"),
    THREE_FOUR("3:4"),
    SIXTEEN_NINE("16:9"),
    NINE_SIXTEEN("9:16"),
    ;

    fun aspect(
        edit: DraftImageEdit,
        width: Int,
        height: Int,
    ): Float? =
        when (this) {
            FREE -> null
            ORIGINAL -> edit.originalOutputAspect(width, height)
            SQUARE -> 1f
            FOUR_THREE -> FOUR / THREE
            THREE_FOUR -> THREE / FOUR
            SIXTEEN_NINE -> SIXTEEN / NINE
            NINE_SIXTEEN -> NINE / SIXTEEN
        }

    fun rotated(): ImageCropPreset =
        when (this) {
            FOUR_THREE -> THREE_FOUR
            THREE_FOUR -> FOUR_THREE
            SIXTEEN_NINE -> NINE_SIXTEEN
            NINE_SIXTEEN -> SIXTEEN_NINE
            else -> this
        }
}

private const val THREE = 3f
private const val FOUR = 4f
private const val NINE = 9f
private const val SIXTEEN = 16f
