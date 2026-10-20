package com.solthariel.pawchan.settings

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.io.File
import java.io.FileOutputStream
import java.nio.file.Files
import java.nio.file.StandardCopyOption

/** Body and validator are committed together, so a failed download cannot poison the ETag. */
internal class ChangelogCache(
    private val file: File,
    private val url: String,
) {
    fun load(): ChangelogDownload? {
        if (!file.isFile) return null
        require(file.length() <= MAX_CACHE_BYTES) { "Cache exceeds limit" }
        val root = Json.parseToJsonElement(file.readText()).jsonObject
        return if (root["url"]?.jsonPrimitive?.contentOrNull == url) {
            val body = requireNotNull(root["body"]?.jsonPrimitive?.contentOrNull)
            decodeRemoteChangelog(body)
            val etag = root["etag"]?.jsonPrimitive?.contentOrNull?.takeIf { it.length <= MAX_CACHED_ETAG }
            ChangelogDownload(body, etag)
        } else {
            null
        }
    }

    fun save(download: ChangelogDownload) {
        val body = requireNotNull(download.body)
        decodeRemoteChangelog(body)
        val text =
            buildJsonObject {
                put("url", JsonPrimitive(url))
                put("body", JsonPrimitive(body))
                put("etag", download.etag?.let(::JsonPrimitive) ?: JsonNull)
            }.toString()
        require(text.toByteArray().size <= MAX_CACHE_BYTES)
        file.parentFile?.mkdirs()
        val temporary = File(file.parentFile, file.name + ".tmp")
        try {
            FileOutputStream(temporary).use {
                it.write(text.toByteArray())
                it.fd.sync()
            }
            Files.move(
                temporary.toPath(),
                file.toPath(),
                StandardCopyOption.ATOMIC_MOVE,
                StandardCopyOption.REPLACE_EXISTING,
            )
        } finally {
            temporary.delete()
        }
    }
}

private const val MAX_CACHE_BYTES = MAX_HISTORY_BYTES * 2
private const val MAX_CACHED_ETAG = 512
