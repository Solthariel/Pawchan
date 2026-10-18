package com.solthariel.pawchan.composer

import com.solthariel.pawchan.domain.settings.ComposerField

internal fun ComposerDraftState.withVisibleFields(hidden: Set<ComposerField>): ComposerDraftState =
    copy(
        email = email.takeUnless { ComposerField.EMAIL in hidden }.orEmpty(),
        subject = subject.takeUnless { ComposerField.SUBJECT in hidden }.orEmpty(),
        options =
            options.copy(
                name = options.name.takeUnless { ComposerField.NAME in hidden }.orEmpty(),
                tag = options.tag.takeUnless { ComposerField.TAG in hidden }.orEmpty(),
            ),
    )
