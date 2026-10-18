package com.solthariel.pawchan.data.network

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

@Serializable
internal data class CatalogDto(
    var board: BoardDto,
    var threads: List<JsonElement>,
    @SerialName("board_speed")
    var boardSpeed: JsonElement? = null,
    var speed: JsonElement? = null,
)

@Serializable
internal data class BoardDto(
    var id: String,
    var name: String,
    @SerialName("enable_subject") var subjectsEnabled: Boolean? = null,
    @SerialName("board_speed")
    var boardSpeed: JsonElement? = null,
    var speed: JsonElement? = null,
)

@Serializable
internal class CatalogThreadDto {
    var num: Long = 0
    var subject: String = ""
    var comment: String = ""
    var name: String? = null
    var date: String? = null
    var timestamp: JsonElement? = null
    var email: String? = null

    @SerialName("posts_count")
    var postsCount: Int = 0

    @SerialName("files_count")
    var filesCount: Int = 0
    var files: List<JsonElement>? = null
    var sticky: Int = 0
    var closed: Int = 0
    var endless: Int = 0
}

@Serializable
internal class AttachmentDto {
    var type: Int? = null
    var md5: String? = null
    var name: String = ""
    var displayname: String? = null
    var path: String = ""
    var thumbnail: String? = null
    var size: Long? = null
    var width: Int? = null
    var height: Int? = null
}
