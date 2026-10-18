package com.solthariel.pawchan.data.network

import com.solthariel.pawchan.domain.model.ThreadAddress
import okhttp3.HttpUrl
import okhttp3.Request

internal object ThreadRequestFactory {
    fun create(address: ThreadAddress): Request =
        Request
            .Builder()
            .url(threadUrl(address))
            .header("Accept", JSON_MEDIA_TYPE)
            .header("User-Agent", USER_AGENT)
            .get()
            .build()

    private fun threadUrl(address: ThreadAddress): HttpUrl =
        HttpUrl
            .Builder()
            .scheme("https")
            .host(DVACH_HOST)
            .addPathSegment(address.board.board.value)
            .addPathSegment(THREAD_DIRECTORY)
            .addPathSegment("${address.thread.value}.json")
            .build()
}

internal const val DVACH_HOST = "2ch.su"
internal const val DVACH_ORIGIN = "https://$DVACH_HOST"
internal const val JSON_MEDIA_TYPE = "application/json"
internal const val USER_AGENT = "Pawchan/0.1"
private const val THREAD_DIRECTORY = "res"
