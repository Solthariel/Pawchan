package com.solthariel.pawchan.composer

import com.solthariel.pawchan.domain.posting.PostingOptions
import com.solthariel.pawchan.domain.posting.PostingOutcome
import com.solthariel.pawchan.domain.posting.PostingRules
import com.solthariel.pawchan.domain.posting.ReplyAttachment

internal typealias AttachmentPreparer =
    suspend (List<ComposerAttachment>, PostingRules) -> PostingOutcome<List<ReplyAttachment>>

data class ComposerFieldActions(
    val onChange: (ComposerDraftState) -> Unit,
    val onClearAll: () -> Unit,
)

internal fun PostingOptions.allowedBy(rules: PostingRules): PostingOptions =
    copy(
        name = if (rules.namesEnabled) name else "",
        sage = sage && rules.sageEnabled,
        icon = icon?.takeIf { selected -> rules.icons.any { it.id == selected } },
        tag = if (rules.tagsEnabled) tag else "",
    )
