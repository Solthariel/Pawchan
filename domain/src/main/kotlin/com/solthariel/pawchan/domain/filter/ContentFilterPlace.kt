package com.solthariel.pawchan.domain.filter

/** Presentation context, never a particular thread number. ANY is for a standalone text probe. */
enum class ContentFilterPlace { ANY, CATALOG, THREAD }

fun ContentFilterRule.appliesIn(place: ContentFilterPlace): Boolean =
    when (place) {
        ContentFilterPlace.ANY -> targets.catalog || targets.thread
        ContentFilterPlace.CATALOG -> targets.catalog
        ContentFilterPlace.THREAD -> targets.thread
    }
