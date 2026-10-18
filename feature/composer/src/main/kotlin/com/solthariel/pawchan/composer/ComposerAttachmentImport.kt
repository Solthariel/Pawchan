package com.solthariel.pawchan.composer

import android.content.Context
import android.net.Uri
import com.solthariel.pawchan.domain.posting.PostingRules
import com.solthariel.pawchan.domain.settings.AttachmentDefaults
import java.io.IOException

internal sealed interface ComposerAttachmentImport {
    data class Success(
        val files: List<ComposerAttachment>,
    ) : ComposerAttachmentImport

    data class Failure(
        val message: String?,
    ) : ComposerAttachmentImport
}

internal suspend fun importComposerAttachments(
    context: Context,
    uris: List<Uri>,
    files: List<ComposerAttachment>,
    rules: PostingRules,
    defaults: AttachmentDefaults = AttachmentDefaults(),
): ComposerAttachmentImport =
    try {
        val imported = readComposerAttachments(context, uris, files, rules)
        val added = imported.drop(files.size).map { it.withDefaults(defaults, rules) }
        ComposerAttachmentImport.Success(files + added)
    } catch (failure: IOException) {
        ComposerAttachmentImport.Failure(failure.message)
    } catch (failure: SecurityException) {
        ComposerAttachmentImport.Failure(failure.message)
    } catch (failure: IllegalArgumentException) {
        ComposerAttachmentImport.Failure(failure.message)
    }
