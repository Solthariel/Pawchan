package com.solthariel.pawchan.composer

import com.solthariel.pawchan.domain.drafts.DraftImageReencoding

/** Reencoded output wins; otherwise JPEG stays JPEG and PNG/WebP become PNG. */
internal fun imageEditIsJpeg(
    sourceFormat: String?,
    encoding: DraftImageReencoding?,
): Boolean = encoding?.jpeg ?: (sourceFormat == "jpg")
