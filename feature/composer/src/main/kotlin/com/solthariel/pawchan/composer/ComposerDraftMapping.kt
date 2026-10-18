package com.solthariel.pawchan.composer

import com.solthariel.pawchan.domain.drafts.ComposerDraftPayload
import com.solthariel.pawchan.domain.drafts.DraftMedia
import com.solthariel.pawchan.domain.drafts.DraftText

fun ComposerDraftState.toStoredDraft(): ComposerDraftPayload =
    ComposerDraftPayload(
        DraftText(subject, comment, email),
        options,
        attachments.map {
            DraftMedia(it.source, it.options, it.dimensions, preview = if (it.video) it.preview else null)
        },
    )

/** File contents/previews are materialized only for the active form, on IO. */
internal fun ComposerDraftPayload.toComposerDraft(): ComposerDraftState =
    ComposerDraftState(
        subject = text.subject,
        comment = text.comment,
        email = text.email,
        options = options,
        attachments =
            media.map { file ->
                val attachment =
                    ComposerAttachment(
                        file.source,
                        sourceUri = file.localUri,
                        options = file.options,
                        dimensions = file.dimensions,
                    )
                if (attachment.video) attachment.copy(preview = file.preview) else attachment
            },
    )
