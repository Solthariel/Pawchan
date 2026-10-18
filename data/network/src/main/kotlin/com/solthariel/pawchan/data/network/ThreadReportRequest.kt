package com.solthariel.pawchan.data.network

import com.solthariel.pawchan.domain.model.PostId
import com.solthariel.pawchan.domain.model.ThreadAddress
import com.solthariel.pawchan.domain.posting.PostingFailure
import com.solthariel.pawchan.domain.posting.PostingOutcome
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.MultipartBody
import okhttp3.Request

internal fun reportPostRequest(
    address: ThreadAddress,
    post: PostId,
    comment: String,
): Request {
    require(comment.isNotBlank())
    val body =
        MultipartBody
            .Builder()
            .setType(MultipartBody.FORM)
            .addFormDataPart("board", address.board.board.value)
            .addFormDataPart("thread", address.thread.value.toString())
            .addFormDataPart("post", post.value.toString())
            .addFormDataPart("comment", comment)
            .build()
    return Request
        .Builder()
        .url("$DVACH_ORIGIN/user/report")
        .header("Accept", "application/json")
        .post(body)
        .build()
}

internal fun decodeReportResponse(payload: String): PostingOutcome<Unit> {
    val json = Json.parseToJsonElement(payload).jsonObject
    return if (json["result"]?.jsonPrimitive?.contentOrNull == "1") {
        PostingOutcome.Success(Unit)
    } else {
        PostingOutcome.Failure(PostingFailure.Rejected(json["message"]?.jsonPrimitive?.contentOrNull))
    }
}
