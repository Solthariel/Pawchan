package com.solthariel.pawchan.settings

import com.solthariel.pawchan.domain.filter.ContentFilterMode
import com.solthariel.pawchan.domain.filter.ContentFilterRule
import com.solthariel.pawchan.domain.filter.ContentFilterSample

internal fun initialFilterProbeText(
    rule: ContentFilterRule,
    samples: List<ContentFilterSample>,
): String =
    if (rule.mode == ContentFilterMode.EXACT_PHRASE) {
        rule.values.singleOrNull().orEmpty()
    } else {
        samples.firstOrNull()?.text.orEmpty()
    }
