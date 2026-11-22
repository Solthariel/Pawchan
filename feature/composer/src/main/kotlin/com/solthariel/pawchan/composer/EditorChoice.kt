package com.solthariel.pawchan.composer

internal data class EditorChoice(
    val label: String,
    val icon: Int? = null,
    val caption: String? = null,
    val selected: Boolean? = null,
)
