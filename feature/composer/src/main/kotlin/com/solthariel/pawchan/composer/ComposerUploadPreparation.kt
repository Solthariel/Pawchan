package com.solthariel.pawchan.composer

import com.solthariel.pawchan.domain.posting.ReplyAttachment
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.coroutines.coroutineContext

/** Transient cache; never serialized in a draft. Keyed by source identity and edit options. */
class ComposerPreparedUpload internal constructor(
    internal val source: ReplyAttachment,
    internal val options: AttachmentEditOptions,
    internal val upload: ReplyAttachment?,
    internal val failure: String? = null,
)

internal val ComposerAttachment.preparedUpload: ReplyAttachment?
    get() {
        if (!validName) return null
        if (prepared?.source === source && prepared.options == options) return prepared.upload
        val transforms = options.uniqueHash || options.removeMetadata || options.reencoding != null
        return if (transforms) null else source.copy(fileName = attachmentUploadName(this), spoiler = options.spoiler)
    }

internal fun sameUploadSelection(
    a: List<ComposerAttachment>,
    b: List<ComposerAttachment>,
): Boolean =
    a.size == b.size &&
        a.zip(b).all { (left, right) ->
            left.id == right.id && left.source === right.source && left.options == right.options
        }

/** Finite, lifecycle-owned work only when the selected bytes/options change. */
internal class ComposerUploadPreparation(
    private val scope: CoroutineScope,
    private val dispatcher: CoroutineDispatcher,
    private val prepare: (ComposerAttachment) -> ReplyAttachment = ::prepareAttachment,
    private val publish: (List<ComposerAttachment>) -> Unit,
) {
    private var selection: List<ComposerAttachment> = emptyList()
    private var job: Job? = null

    fun update(files: List<ComposerAttachment>) {
        if (sameUploadSelection(selection, files)) return
        selection = files.toList()
        job?.cancel()
        if (files.none { it.preparedUpload == null && it.validName }) return
        job =
            scope.launch {
                val result = withContext(dispatcher) { prepareSelection(files, prepare) }
                coroutineContext.ensureActive()
                if (sameUploadSelection(selection, files)) publish(result)
            }
    }
}

private suspend fun prepareSelection(
    files: List<ComposerAttachment>,
    prepare: (ComposerAttachment) -> ReplyAttachment,
): List<ComposerAttachment> {
    var used = 0L
    return files.map { file ->
        coroutineContext.ensureActive()
        val result = if (file.preparedUpload != null || !file.validName) file else prepareUploadCache(file, prepare)
        used += result.preparedUpload?.content?.size ?: 0
        if (used > com.solthariel.pawchan.domain.posting.PostingRules.MAX_TOTAL_BYTES) {
            file.copy(
                prepared =
                    ComposerPreparedUpload(
                        file.source,
                        file.options,
                        null,
                        "После обработки превышен локальный лимит памяти вложений",
                    ),
            )
        } else {
            result
        }
    }
}

private fun prepareUploadCache(
    file: ComposerAttachment,
    prepare: (ComposerAttachment) -> ReplyAttachment,
): ComposerAttachment {
    val cache =
        try {
            ComposerPreparedUpload(file.source, file.options, prepare(file))
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: IllegalArgumentException) {
            ComposerPreparedUpload(file.source, file.options, null, ATTACHMENT_PROCESSING_ERROR)
        } catch (_: IllegalStateException) {
            ComposerPreparedUpload(file.source, file.options, null, ATTACHMENT_PROCESSING_ERROR)
        } catch (_: java.io.IOException) {
            ComposerPreparedUpload(file.source, file.options, null, ATTACHMENT_PROCESSING_ERROR)
        }
    return file.copy(prepared = cache)
}
