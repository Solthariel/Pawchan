package com.solthariel.pawchan.data.network

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

@Serializable
internal data class ThreadResponseDto(
    var threads: List<ThreadContainerDto>,
    var board: ThreadBoardDto? = null,
    @SerialName("unique_posters") var uniquePosters: Int = 0,
)

@Serializable
internal data class ThreadContainerDto(
    var posts: List<JsonElement>,
)

@Serializable
internal class ThreadPostDto {
    var num: Long = 0
    var parent: Long = 0
    var subject: String = ""
    var comment: String = ""
    var name: String? = null
    var date: String? = null
    var icon: String? = null
    var op: JsonElement? = null
    var email: String? = null
    var files: List<JsonElement>? = null
    var sticky: Int = 0
    var closed: Int = 0
    var endless: Int = 0
    var likes: Int? = null
    var dislikes: Int? = null
    var timestamp: Long = 0
    var trip: String? = null
    var banned: Int = 0
    var tags: String? = null
}
