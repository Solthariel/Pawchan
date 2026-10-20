package com.solthariel.pawchan.thread

/** Navigation is relative to the visible post, not a reset result index; wraps at either end. */
internal fun threadSearchTarget(
    results: List<Long>,
    positions: Map<Long, Int>,
    anchor: Int,
    delta: Int,
): Long? {
    val ordered = results.mapNotNull { id -> positions[id]?.let { id to it } }.sortedBy { it.second }
    return if (delta > 0) {
        (ordered.firstOrNull { it.second > anchor } ?: ordered.firstOrNull())?.first
    } else {
        (ordered.lastOrNull { it.second < anchor } ?: ordered.lastOrNull())?.first
    }
}
