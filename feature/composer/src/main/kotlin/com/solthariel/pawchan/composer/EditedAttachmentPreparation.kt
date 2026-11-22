package com.solthariel.pawchan.composer

import com.solthariel.pawchan.data.network.PostingMediaBytes
import com.solthariel.pawchan.domain.drafts.DraftImageEdit
import com.solthariel.pawchan.domain.drafts.DraftImageReencoding
import com.solthariel.pawchan.domain.posting.ReplyAttachment

/** Export must succeed and match the expected format; there is no fallback to source bytes. */
internal fun prepareEditedAttachment(
    file: ComposerAttachment,
    export: (ByteArray, DraftImageEdit, DraftImageReencoding?) -> ImageEditOutput,
): ReplyAttachment {
    val edit = requireNotNull(file.options.edit)
    val rendered = export(file.source.content, edit, file.options.reencoding)
    val jpeg = imageEditIsJpeg(file.format, file.options.reencoding)
    val extension = if (jpeg) "jpg" else "png"
    val mime = if (jpeg) "image/jpeg" else "image/png"
    require(rendered.extension == extension && rendered.mime == mime)
    require(PostingMediaBytes.format(rendered.content) == extension)
    val clean = PostingMediaBytes.removeMetadata(rendered.content)
    val unique = PostingMediaBytes.unique(clean)
    return ReplyAttachment(attachmentUploadName(file), mime, unique, spoiler = file.options.spoiler)
}
