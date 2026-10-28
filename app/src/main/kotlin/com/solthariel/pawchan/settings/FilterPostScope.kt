package com.solthariel.pawchan.settings

import com.solthariel.pawchan.domain.filter.ContentFilterTargets

internal enum class FilterPostScope(
    val label: String,
) {
    ALL("Все посты"),
    OP("ОП-посты"),
    COMMENTS("Комментарии"),
}

internal fun ContentFilterTargets.postScope(): String =
    when {
        threads && !comments -> FilterPostScope.OP.name
        comments && !threads -> FilterPostScope.COMMENTS.name
        else -> FilterPostScope.ALL.name
    }
