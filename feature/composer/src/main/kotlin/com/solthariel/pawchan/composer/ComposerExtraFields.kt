package com.solthariel.pawchan.composer

import com.solthariel.pawchan.domain.posting.PostingRules
import com.solthariel.pawchan.domain.settings.ComposerField

internal fun availableComposerFields(
    rules: PostingRules?,
    newThread: Boolean,
    hidden: Set<ComposerField>,
): List<ComposerField> {
    if (rules == null) return emptyList()
    return buildList {
        if (newThread && rules.subjectEnabled) add(ComposerField.SUBJECT)
        if (newThread && rules.tagsEnabled) add(ComposerField.TAG)
        if (rules.namesEnabled) add(ComposerField.NAME)
        add(ComposerField.EMAIL)
    }.filterNot { it in hidden }
}

internal fun ComposerDraftState.fieldValue(field: ComposerField): String =
    when (field) {
        ComposerField.NAME -> options.name
        ComposerField.EMAIL -> email
        ComposerField.SUBJECT -> subject
        ComposerField.TAG -> tag
    }

internal fun ComposerDraftState.withFieldValue(
    field: ComposerField,
    value: String,
): ComposerDraftState =
    when (field) {
        ComposerField.NAME -> copy(options = options.copy(name = value))
        ComposerField.EMAIL -> copy(email = value)
        ComposerField.SUBJECT -> copy(subject = value)
        ComposerField.TAG -> copy(options = options.copy(tag = value))
    }

internal fun filledComposerFieldCount(
    draft: ComposerDraftState,
    fields: List<ComposerField>,
): Int = fields.count { draft.fieldValue(it).isNotBlank() }
