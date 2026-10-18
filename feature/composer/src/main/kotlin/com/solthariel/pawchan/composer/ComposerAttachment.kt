package com.solthariel.pawchan.composer

import com.solthariel.pawchan.core.ui.formatByteSizeForUi
import com.solthariel.pawchan.data.network.PostingMediaBytes
import com.solthariel.pawchan.domain.posting.ReplyAttachment
import com.solthariel.pawchan.media.canonicalMediaFormat
import com.solthariel.pawchan.media.metadata.basicMediaSummary
import java.util.UUID

data class ComposerAttachment(
    val source: ReplyAttachment,
    val sourceUri: String? = null,
    val preview: ByteArray? = source.content,
    val options: AttachmentEditOptions = AttachmentEditOptions(),
    val id: String = UUID.randomUUID().toString(),
    val dimensions: String? = null,
) {
    val format: String? = PostingMediaBytes.format(source.content)
    val aspectRatio: Float = composerMediaAspectRatio(dimensions)
    val video: Boolean = format == "mp4" || format == "webm"
    val canStripMetadata: Boolean = format == "jpg" || format == "png"
    val canReencode: Boolean = format in setOf("jpg", "png", "webp")
    val validName: Boolean get() =
        options.removeFileName ||
            validAttachmentName(options.customName ?: source.fileName.substringBeforeLast('.', source.fileName))
    val displayName: String = if (validName) attachmentUploadName(this) else source.fileName
    val displaySize: String = formatByteSizeForUi(source.content.size.toLong())
    val displayFormat: String = canonicalMediaFormat(format ?: source.fileName.substringAfterLast('.', ""))
    val typeAndSize: String = "$displayFormat $displaySize"

    // Same order as the viewer summary; original file, not a prediction of the processed upload.
    val metadataFallback: String = basicMediaSummary(displayFormat, dimensions, source.content.size.toLong())
}

internal typealias AttachmentEditOptions = com.solthariel.pawchan.domain.drafts.MediaDraftOptions
internal typealias ImageReencoding = com.solthariel.pawchan.domain.drafts.DraftImageReencoding

internal fun attachmentUploadName(file: ComposerAttachment): String {
    val extension =
        file.options.reencoding?.let { if (it.jpeg) "jpg" else "png" }
            ?: file.source.fileName.substringAfterLast('.', "")
    val base =
        when {
            file.options.removeFileName -> "file"
            file.options.customName != null -> requireNotNull(file.options.customName)
            else -> file.source.fileName.substringBeforeLast('.', file.source.fileName)
        }
    require(validAttachmentName(base)) { "Недопустимое имя файла" }
    return if (extension.isEmpty()) base else "$base.$extension"
}

internal fun validAttachmentName(name: String): Boolean =
    name.isNotBlank() && name.length <= MAX_UPLOAD_NAME &&
        name.none { it == '/' || it == '\\' || it.isISOControl() }

private const val MAX_UPLOAD_NAME = 120

private fun composerMediaAspectRatio(dimensions: String?): Float {
    val sides = dimensions?.split('×') ?: return 1f
    val width = sides.getOrNull(0)?.toFloatOrNull() ?: 0f
    val height = sides.getOrNull(1)?.toFloatOrNull() ?: 0f
    val validDimensions = listOf(width, height).all { it > 0 && it.isFinite() }
    return if (validDimensions) (width / height).takeIf { it > 0 && it.isFinite() } ?: 1f else 1f
}
