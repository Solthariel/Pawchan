package com.solthariel.pawchan.settings

import androidx.compose.runtime.Composable
import com.solthariel.pawchan.domain.filter.ContentFilterMode

/** Preserve and expose non-default legacy restrictions without a compulsory extra page. */
@Composable
internal fun FilterLegacyFields(draft: ContentFilterDraft) {
    if (!draft.catalog || !draft.thread) {
        FilterCheck("В каталоге", draft.catalog, { draft.catalog = it })
        FilterCheck("В открытом треде", draft.thread, { draft.thread = it })
    }
    if (draft.mode == ContentFilterMode.ORDERED.name) {
        FilterLineInput("Расстояние между фрагментами", draft.gap, { draft.gap = it })
    }
    if (draft.mode == ContentFilterMode.ALL_WORDS.name && !draft.wholeWords) {
        FilterCheck("Целые слова вместо частей слов", draft.wholeWords, { draft.wholeWords = it })
    }
    if (draft.label.isNotBlank()) FilterLineInput("Название", draft.label, { draft.label = it })
    if (draft.exceptions.isNotBlank()) FilterLineInput("Исключения", draft.exceptions, { draft.exceptions = it })
    if (draft.expires.isNotBlank()) FilterLineInput("Дата отключения", draft.expires, { draft.expires = it })
}
