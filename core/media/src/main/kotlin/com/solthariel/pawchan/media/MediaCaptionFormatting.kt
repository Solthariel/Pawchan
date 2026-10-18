package com.solthariel.pawchan.media

import java.util.Locale

internal fun attachmentFormat(
    originalUrl: String,
    fileName: String,
): String {
    val originalName = originalUrl.substringBefore('?').substringBefore('#').substringAfterLast('/')
    val extension = originalName.substringAfterLast('.', "").ifBlank { fileName.substringAfterLast('.', "") }
    return canonicalMediaFormat(extension)
}

fun canonicalMediaFormat(extension: String): String =
    when (val format = extension.uppercase(Locale.ROOT)) {
        "JPG", "JPEG" -> "JPEG"
        "" -> "ФАЙЛ"
        else -> format
    }

internal fun attachmentTypeAndSize(attachment: MediaAttachmentUi): String = attachment.display.typeAndSize
