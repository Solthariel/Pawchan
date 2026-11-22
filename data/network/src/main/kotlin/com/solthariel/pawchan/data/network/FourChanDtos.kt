package com.solthariel.pawchan.data.network

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
internal data class FourChanBoardsDto(
    val boards: List<FourChanBoardDto>,
)

@Serializable
internal data class FourChanBoardDto(
    val board: String,
    val title: String,
    @SerialName("ws_board") val worksafe: Int = 0,
)

@Serializable
internal data class FourChanCatalogPageDto(
    val page: Int,
    val threads: List<FourChanPostDto>,
)

@Serializable
internal data class FourChanThreadDto(
    val posts: List<FourChanPostDto>,
)

@Serializable
internal class FourChanPostDto {
    var no: Long = 0
    var resto: Long = 0
    var name: String? = null
    var trip: String? = null
    var email: String? = null
    var sub: String? = null
    var com: String? = null
    var now: String? = null
    var time: Long = 0
    var sticky: Int = 0
    var closed: Int = 0
    var archived: Int = 0
    var replies: Int = 0
    var images: Int = 0
    var filename: String? = null
    var ext: String? = null
    var tim: Long? = null
    var fsize: Long? = null
    var w: Int? = null
    var h: Int? = null
    var spoiler: Int = 0
    var filedeleted: Int = 0
}
