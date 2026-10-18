package com.solthariel.pawchan.composer

import androidx.compose.runtime.staticCompositionLocalOf
import com.solthariel.pawchan.domain.drafts.DraftImageReencoding
import com.solthariel.pawchan.domain.drafts.MediaDraftOptions
import com.solthariel.pawchan.domain.posting.PostingRules
import com.solthariel.pawchan.domain.settings.AttachmentDefaults
import com.solthariel.pawchan.domain.settings.ComposerPreferences
import com.solthariel.pawchan.domain.settings.ReencodeFormat
import com.solthariel.pawchan.domain.settings.UploadNameMode
import java.util.UUID

/** Explicit app-host binding; no repository or workspace is obtained from the UI. */
val LocalComposerPreferences = staticCompositionLocalOf { ComposerPreferences() }

internal fun ComposerAttachment.withDefaults(
    defaults: AttachmentDefaults,
    rules: PostingRules,
): ComposerAttachment {
    val name = defaults.naming
    val encoding = defaults.reencoding
    val target =
        when (encoding.format) {
            ReencodeFormat.ORIGINAL -> null
            ReencodeFormat.JPEG -> "jpg"
            ReencodeFormat.PNG -> "png"
        }
    val reencode = target != null && canReencode && target in rules.extensions
    return copy(
        options =
            MediaDraftOptions(
                uniqueHash = defaults.uniqueHash && format != null,
                removeMetadata = defaults.removeMetadata && canStripMetadata,
                spoiler = defaults.spoiler && rules.features.attachmentSpoilers,
                removeFileName = name.mode == UploadNameMode.REMOVE,
                customName =
                    when (name.mode) {
                        UploadNameMode.CUSTOM -> name.customBase.takeIf(::validAttachmentName) ?: "image"
                        UploadNameMode.RANDOM -> UUID.randomUUID().toString()
                        else -> null
                    },
                reencoding =
                    if (reencode) {
                        DraftImageReencoding(
                            jpeg = encoding.format == ReencodeFormat.JPEG,
                            quality = encoding.quality.coerceIn(1, MAX_IMAGE_QUALITY),
                            scaleDivisor = encoding.divisor.takeIf { it in IMAGE_DIVISORS } ?: 1,
                        )
                    } else {
                        null
                    },
            ),
    )
}

private const val MAX_IMAGE_QUALITY = 100
private const val QUARTER_DIVISOR = 4
private const val EIGHTH_DIVISOR = 8
private val IMAGE_DIVISORS = listOf(1, 2, QUARTER_DIVISOR, EIGHTH_DIVISOR)
