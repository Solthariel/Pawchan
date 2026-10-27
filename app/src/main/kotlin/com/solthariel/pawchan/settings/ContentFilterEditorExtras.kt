package com.solthariel.pawchan.settings

import androidx.compose.runtime.Composable

/** Preserve and expose non-default legacy restrictions without a compulsory extra page. */
@Composable
internal fun FilterLegacyFields(draft: ContentFilterDraft) {
    if (!draft.catalog || !draft.thread) {
        FilterCheck("В каталоге", draft.catalog, { draft.catalog = it })
        FilterCheck("В открытом треде", draft.thread, { draft.thread = it })
    }
    if (draft.label.isNotBlank()) FilterLineInput("Название", draft.label, { draft.label = it })
    if (draft.exceptions.isNotBlank()) FilterLineInput("Исключения", draft.exceptions, { draft.exceptions = it })
    if (draft.expires.isNotBlank()) FilterLineInput("Дата отключения", draft.expires, { draft.expires = it })
}
