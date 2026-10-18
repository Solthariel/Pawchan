package com.solthariel.pawchan.data.drafts

import com.solthariel.pawchan.domain.drafts.DraftText
import com.solthariel.pawchan.domain.drafts.MediaDraftOptions
import com.solthariel.pawchan.domain.posting.PostingOptions

internal data class DraftRecord(
    val key: String,
    val text: DraftText,
    val options: PostingOptions,
    val media: List<CachedDraftMedia>,
)

internal data class CachedDraftMedia(
    val hash: String,
    val name: String,
    val mime: String?,
    val bytes: Long,
    val options: MediaDraftOptions,
    val dimensions: String?,
    val preview: CachedDraftPreview? = null,
)

internal data class CachedDraftPreview(
    val hash: String,
    val bytes: Long,
)
