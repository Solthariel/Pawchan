package com.solthariel.pawchan.composer

import com.solthariel.pawchan.domain.model.BoardPost
import com.solthariel.pawchan.domain.model.PostAddress
import com.solthariel.pawchan.domain.model.PostContent
import com.solthariel.pawchan.domain.model.PostDocument
import com.solthariel.pawchan.domain.model.PostId
import com.solthariel.pawchan.domain.model.PostMetadata
import com.solthariel.pawchan.domain.model.ThreadAddress
import com.solthariel.pawchan.domain.posting.ReplyDraft

/** Local submitted-text preview, not a server snapshot; remote attachments are not known yet. */
internal fun postedBoardPost(
    address: ThreadAddress,
    id: PostId,
    draft: ReplyDraft,
): BoardPost =
    BoardPost(
        PostAddress(address, id),
        PostContent(PostDocument.plain(draft.comment), PostMetadata(null, null, draft.email == "sage")),
        emptyList(),
    )

internal fun postedNewThread(
    address: ThreadAddress,
    draft: com.solthariel.pawchan.domain.posting.NewThreadDraft,
): BoardPost =
    postedBoardPost(
        address,
        requireNotNull(PostId.parse(address.thread.value)),
        ReplyDraft(
            draft.comment,
            draft.emojiCaptchaToken,
            draft.attachments,
            draft.email,
            draft.options,
        ),
    )
