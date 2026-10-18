package com.solthariel.pawchan.data.network

import com.solthariel.pawchan.domain.boards.ArchivedThreadSummary
import com.solthariel.pawchan.domain.boards.BoardArchiveSnapshot
import com.solthariel.pawchan.domain.boards.BoardConfiguration
import com.solthariel.pawchan.domain.boards.BoardInformation
import com.solthariel.pawchan.domain.model.BoardAddress
import com.solthariel.pawchan.domain.model.ThreadAddress
import com.solthariel.pawchan.domain.model.ThreadId
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.longOrNull

internal class BoardDetailsPayloadDecoder(
    private val json: Json,
) {
    fun archive(
        board: BoardAddress,
        payload: String,
    ): BoardArchiveSnapshot? {
        val root = json.parseToJsonElement(payload) as? JsonObject
        val rows = root?.get("threads") as? JsonArray ?: return null
        val threads = rows.mapNotNull { (it as? JsonObject)?.toArchivedThread(board) }.distinctBy { it.address }
        val pages =
            (root?.get("pages") as? JsonArray)
                .orEmpty()
                .mapNotNull { (it as? JsonPrimitive)?.intOrNull?.takeIf { page -> page >= 0 } }
                .distinct()
        return BoardArchiveSnapshot(threads, pages)
    }

    fun information(
        address: BoardAddress,
        payload: String,
    ): BoardInformation? {
        val root = json.parseToJsonElement(payload) as? JsonObject
        val board = root?.get("board") as? JsonObject
        val title = board?.text("name")?.decodeHtmlText()?.takeIf(String::isNotBlank)
        return if (board == null || title == null || board.text("id") != address.board.value) {
            null
        } else {
            BoardInformation(
                address = address,
                title = title,
                description = PostDocumentParser.parse(board.text("info").orEmpty()).plainText,
                defaultName = board.text("default_name"),
                bumpLimit = board.positiveInt("bump_limit"),
                maxCommentLength = board.positiveInt("max_comment"),
                configuration = BoardConfiguration(board.flag("enable_subject"), board.flag("enable_names")),
            )
        }
    }
}

private fun JsonObject.toArchivedThread(board: BoardAddress): ArchivedThreadSummary? {
    val id = (get("thread") as? JsonPrimitive)?.longOrNull?.let(ThreadId::parse) ?: return null
    val subject = text("subject")?.decodeHtmlText()?.trim()?.takeIf { it.isNotEmpty() && it != "Нет темы" }
    return ArchivedThreadSummary(ThreadAddress(board, id), subject ?: "#${id.value}")
}

private fun JsonObject.text(key: String): String? = (get(key) as? JsonPrimitive)?.contentOrNull

private fun JsonObject.positiveInt(key: String): Int? = (get(key) as? JsonPrimitive)?.intOrNull?.takeIf { it > 0 }
