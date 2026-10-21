package com.solthariel.pawchan.composer

import com.solthariel.pawchan.data.network.PostingMediaBytes
import com.solthariel.pawchan.domain.posting.PostingFailure
import com.solthariel.pawchan.domain.posting.PostingOutcome
import com.solthariel.pawchan.domain.posting.PostingRules
import com.solthariel.pawchan.domain.posting.ReplyAttachment
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import java.io.IOException
import kotlin.coroutines.coroutineContext

internal suspend fun prepareComposerAttachments(
    files: List<ComposerAttachment>,
    rules: PostingRules,
): PostingOutcome<List<ReplyAttachment>> {
    val rejection = composerAttachmentRejection(files, rules)
    return when {
        files.isEmpty() -> {
            PostingOutcome.Success(emptyList())
        }

        rejection != null -> {
            PostingOutcome.Failure(PostingFailure.Rejected(rejection))
        }

        else -> {
            withContext(Dispatchers.IO) {
                try {
                    val prepared =
                        files.map { file ->
                            coroutineContext.ensureActive()
                            (file.preparedUpload ?: prepareAttachment(file)).let {
                                it.copy(spoiler = it.spoiler && rules.features.attachmentSpoilers)
                            }
                        }
                    require(rules.accepts(prepared)) {
                        "После обработки превышен лимит файлов или выбран недопустимый формат"
                    }
                    PostingOutcome.Success(prepared)
                } catch (cancelled: CancellationException) {
                    throw cancelled
                } catch (failure: IllegalArgumentException) {
                    PostingOutcome.Failure(PostingFailure.Rejected(failure.message))
                } catch (failure: IllegalStateException) {
                    PostingOutcome.Failure(PostingFailure.Rejected(failure.message))
                } catch (failure: IOException) {
                    PostingOutcome.Failure(PostingFailure.Rejected(failure.message))
                }
            }
        }
    }
}

private fun composerAttachmentRejection(
    files: List<ComposerAttachment>,
    rules: PostingRules,
): String? =
    when {
        files.size > rules.maxFiles -> {
            "Этот набор вложений превышает текущий лимит аккаунта. Черновик сохранён."
        }

        else -> {
            null
        }
    }

internal fun prepareAttachment(file: ComposerAttachment): ReplyAttachment {
    val options = file.options
    var content = file.source.content
    val reencoding = options.reencoding
    if (reencoding != null) {
        content = reencodeAttachmentImage(content, reencoding)
    } else if (options.removeMetadata && imageOrientation(content) != 1) {
        // Bake EXIF orientation before removing it; otherwise portrait photos would turn sideways.
        content = reencodeAttachmentImage(content, ImageReencoding(jpeg = file.format == "jpg", quality = 100))
    }
    if (options.removeMetadata) content = PostingMediaBytes.removeMetadata(content)
    if (options.uniqueHash) content = PostingMediaBytes.unique(content)
    val mime = options.reencoding?.let { if (it.jpeg) "image/jpeg" else "image/png" } ?: file.source.mimeType
    return ReplyAttachment(attachmentUploadName(file), mime, content, spoiler = options.spoiler)
}
