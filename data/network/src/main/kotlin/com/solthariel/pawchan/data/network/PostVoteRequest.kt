package com.solthariel.pawchan.data.network

import com.solthariel.pawchan.domain.model.PostAddress
import com.solthariel.pawchan.domain.posting.PostingFailure
import com.solthariel.pawchan.domain.posting.PostingOutcome
import com.solthariel.pawchan.domain.thread.PostVote
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.Request

/** Как в board.js 2ch: GET /api/like|/api/dislike?board=&num=, ответ {"result":1} или {"result":0,"error":{...}}. */
internal fun postVoteRequest(
    post: PostAddress,
    vote: PostVote,
): Request {
    val path = if (vote == PostVote.LIKE) "like" else "dislike"
    val url =
        "$DVACH_ORIGIN/api/$path"
            .toHttpUrl()
            .newBuilder()
            .addQueryParameter("board", post.thread.board.board.value)
            .addQueryParameter("num", post.post.value.toString())
            .build()
    return Request
        .Builder()
        .url(url)
        .header("Accept", "application/json")
        .get()
        .build()
}

internal fun decodePostVoteResponse(payload: String): PostingOutcome<Unit> {
    val json = Json.parseToJsonElement(payload) as? JsonObject
    val error = json?.get("error") as? JsonObject
    val message = (error ?: json)?.get("message")?.jsonPrimitive?.contentOrNull
    val code = error?.get("code")?.jsonPrimitive?.contentOrNull
    val reason =
        when {
            message.isNullOrBlank() -> null
            code.isNullOrBlank() -> message
            else -> "Ошибка $code: $message"
        }
    return when {
        json == null -> PostingOutcome.Failure(PostingFailure.InvalidResponse)
        json["result"]?.jsonPrimitive?.contentOrNull == "1" -> PostingOutcome.Success(Unit)
        else -> PostingOutcome.Failure(PostingFailure.Rejected(reason))
    }
}
