package com.solthariel.pawchan.data.network

import com.solthariel.pawchan.domain.model.BoardId
import okhttp3.HttpUrl
import okhttp3.Request

internal object CatalogRequestFactory {
    fun create(board: BoardId): Request = create(board, CATALOG_FILE)

    private fun create(
        board: BoardId,
        file: String,
    ): Request =
        Request
            .Builder()
            .url(boardUrl(board, file))
            .header("Accept", JSON_MEDIA_TYPE)
            .header("User-Agent", USER_AGENT)
            .get()
            .build()

    private fun boardUrl(
        board: BoardId,
        file: String,
    ): HttpUrl =
        HttpUrl
            .Builder()
            .scheme("https")
            .host(DVACH_HOST)
            .addPathSegment(board.value)
            .addPathSegment(file)
            .build()
}

private const val CATALOG_FILE = "catalog.json"
