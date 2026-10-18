package com.solthariel.pawchan.data.drafts

import com.solthariel.pawchan.domain.archive.ArchivedMedia
import com.solthariel.pawchan.domain.workspace.ThreadCopy
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.coroutines.executeAsync
import java.io.File
import java.io.IOException
import java.security.MessageDigest

/**
 * Durable, private files; no dependency on Coil's evictable cache. Sequential downloads with a pause between files:
 * a burst of requests makes the server (or VPN) reset the stream.
 */
class ArchiveMediaStore(
    private val directory: File,
    private val client: OkHttpClient,
    private val pauseMillis: Long = DOWNLOAD_PAUSE_MILLIS,
) {
    suspend fun download(
        copy: ThreadCopy,
        onFile: suspend (ArchivedMedia) -> Unit = {},
    ): List<ArchivedMedia> =
        withContext(Dispatchers.IO) {
            if (!directory.isDirectory && !directory.mkdirs()) throw IOException("Не удалось создать папку архива")
            val old = copy.archive.files.associateBy { it.url }
            var downloaded = false
            copy.snapshot.posts.flatMap { it.attachments }.distinctBy { it.remoteUrl }.map { media ->
                val url = media.remoteUrl.value
                val stored = old[url]?.takeIf { resolve(it)?.length() == it.bytes && it.bytes > 0 }
                stored ?: run {
                    if (downloaded) delay(pauseMillis)
                    downloaded = true
                    downloadOne(url).also { onFile(it) }
                }
            }
        }

    fun resolve(media: ArchivedMedia): File? {
        if (!SAFE_FILE.matches(media.file)) return null
        return File(directory, media.file)
    }

    suspend fun prune(retained: Set<String>) =
        withContext(Dispatchers.IO) {
            directory
                .listFiles()
                ?.filter {
                    SAFE_FILE.matches(
                        it.name,
                    ) && it.name !in retained
                }?.forEach { it.delete() }
            Unit
        }

    private fun archiveMediaUrl(url: String): okhttp3.HttpUrl {
        val parsed =
            okhttp3.HttpUrl.Companion.run { url.toHttpUrlOrNull() }
                ?: throw IOException("Неверный адрес медиа")
        if (parsed.scheme != "https" ||
            parsed.host !in MEDIA_ARCHIVE_HOSTS
        ) {
            throw IOException("Неподдерживаемый хост медиа")
        }
        return parsed
    }

    private suspend fun downloadOne(url: String): ArchivedMedia {
        val parsed = archiveMediaUrl(url)
        val name = MessageDigest.getInstance("SHA-256").digest(url.toByteArray()).joinToString("") { "%02x".format(it) }
        val target = File(directory, name)
        if (target.isFile && target.length() > 0) return ArchivedMedia(url, name, target.length())
        val part = File.createTempFile("download-", ".part", directory)
        try {
            val bytes = downloadArchiveResponse(client.newCall(Request.Builder().url(parsed).build()), part)
            if (!part.renameTo(target)) throw IOException("Не удалось сохранить медиа")
            return ArchivedMedia(url, name, bytes)
        } finally {
            part.delete()
        }
    }
}

private val SAFE_FILE = Regex("[a-f0-9]{64}")
private val MEDIA_ARCHIVE_HOSTS = setOf("2ch.su", "2ch.hk", "2ch.life", "2ch.pm", "arhivach.vc")

private const val DOWNLOAD_PAUSE_MILLIS = 1_000L
