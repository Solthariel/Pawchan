package com.solthariel.pawchan.data.network

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
internal data class ThreadBoardDto(
    @SerialName("enable_subject") val subjectsEnabled: Boolean? = null,
    @SerialName("enable_likes") val likesEnabled: Boolean? = null,
)
