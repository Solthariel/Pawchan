package com.solthariel.pawchan.data.drafts

import com.solthariel.pawchan.domain.model.MediaKind
import com.solthariel.pawchan.domain.workspace.ThreadCopy
import java.io.OutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

class ArchiveZipWriter(
    private val media: ArchiveMediaStore,
) {
    suspend fun write(
        copy: ThreadCopy,
        output: OutputStream,
    ) {
        val files =
            copy.archive.files.filter { file ->
                media.resolve(file)?.let { it.isFile && it.length() == file.bytes } == true
            }
        val exported = copy.copy(archive = copy.archive.copy(media = copy.archive.media.copy(files = files)))
        ZipOutputStream(output).use { zip ->
            zip.putNextEntry(ZipEntry("snapshot.json"))
            zip.write(exported.encodeCopy().toString().toByteArray())
            zip.closeEntry()
            zip.putNextEntry(ZipEntry("index.html"))
            zip.write(archiveHtml(exported).toByteArray())
            zip.closeEntry()
            files.distinctBy { it.file }.forEach { item ->
                val file = media.resolve(item)
                if (file?.isFile == true && file.length() == item.bytes) {
                    zip.putNextEntry(ZipEntry("media/${item.file}"))
                    file.inputStream().use { archiveCopyBytes(it, zip, item.bytes) }
                    zip.closeEntry()
                }
            }
        }
    }
}

private fun archiveHtml(copy: ThreadCopy): String =
    buildString {
        append(
            "<!doctype html><html lang=ru><meta charset=utf-8><meta name=viewport content='width=device-width'><title>",
        )
        append(escapeArchiveHtml(copy.snapshot.subject.orEmpty()))
        append(
            "</title><style>body{max-width:900px;margin:16px auto;background:#17151d;color:#eee}" +
                "article{padding:12px;border-bottom:1px solid #777}pre{white-space:pre-wrap}" +
                "img,video{max-width:100%;max-height:70vh}a{color:#b9a4ea}</style>",
        )
        append("<p>Локальная копия. Полнота постов и медиа не гарантируется.</p>")
        val files = copy.archive.files.associateBy { it.url }
        copy.snapshot.posts.forEach { post ->
            append("<article id='post${post.address.post.value}'><b>№${post.address.post.value}</b><pre>")
            append(escapeArchiveHtml(post.content.document.plainText))
            append("</pre>")
            post.attachments.forEach { item ->
                val local = files[item.remoteUrl.value]?.file
                val url = escapeArchiveHtml(local?.let { "media/$it" } ?: item.remoteUrl.value)
                append("<p><a href='$url'>${escapeArchiveHtml(item.fileName)}</a>")
                if (local != null) {
                    append(
                        when (item.kind) {
                            MediaKind.IMAGE, MediaKind.ANIMATION -> "<br><img src='$url'>"
                            MediaKind.VIDEO -> "<br><video controls src='$url'></video>"
                            else -> ""
                        },
                    )
                }
                append("</p>")
            }
            append("</article>")
        }
        append("</html>")
    }

private fun escapeArchiveHtml(value: String): String =
    value
        .replace("&", "&amp;")
        .replace("<", "&lt;")
        .replace(">", "&gt;")
        .replace("\"", "&quot;")
        .replace("'", "&#39;")
