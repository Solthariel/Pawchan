package com.solthariel.pawchan.domain.drafts

import com.solthariel.pawchan.domain.model.BoardAddress
import com.solthariel.pawchan.domain.model.ThreadId
import com.solthariel.pawchan.domain.posting.PostingOptions
import com.solthariel.pawchan.domain.posting.ReplyAttachment

/** null thread means a new thread; a reply never shares that draft. */
data class ComposerDraftKey(
    val board: BoardAddress,
    val thread: ThreadId? = null,
)

data class DraftText(
    val subject: String = "",
    val comment: String = "",
    val email: String = "",
)

data class DraftMedia(
    val source: ReplyAttachment,
    val options: MediaDraftOptions = MediaDraftOptions(),
    val dimensions: String? = null,
    val localUri: String? = null,
    val preview: ByteArray? = null,
)

data class ComposerDraftPayload(
    val text: DraftText = DraftText(),
    val options: PostingOptions = PostingOptions(),
    val media: List<DraftMedia> = emptyList(),
) {
    val empty: Boolean get() = text == DraftText() && options == PostingOptions() && media.isEmpty()
}

/** Implementations keep bounded metadata/file references, not a cache of media byte arrays. */
interface ComposerDraftRepository {
    suspend fun load(key: ComposerDraftKey): ComposerDraftPayload?

    suspend fun save(
        key: ComposerDraftKey,
        draft: ComposerDraftPayload,
    )

    suspend fun remove(key: ComposerDraftKey)
}
