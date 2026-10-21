package com.solthariel.pawchan.domain.filter

import java.util.PriorityQueue

/** Occurrence events share predecessor nodes; full chains are materialized only for the result. */
internal class OrderedFilterMatcher(
    private val values: List<String>,
    private val maxGap: Int?,
) {
    fun ranges(text: String): List<IntRange>? {
        if (values.sumOf { it.length.toLong() } > text.length) return null
        return if (maxGap == null) greedy(text) else bounded(text, maxGap)
    }

    private fun greedy(text: String): List<IntRange>? {
        var start = 0
        return values.map { value ->
            val offset = text.indexOf(value, start)
            if (offset < 0) return null
            start = offset + value.length
            offset until start
        }
    }

    private fun bounded(
        text: String,
        gap: Int,
    ): List<IntRange>? {
        val events = events(text)
        if (events != null) {
            val windows = values.drop(1).map { OrderedReachabilityWindow(it.length) }
            while (events.isNotEmpty()) {
                val event = events.remove()
                val parent = if (event.stage == 0) null else windows[event.stage - 1].parent(event.range.first, gap)
                if (event.stage == 0 || parent != null) {
                    val node = OrderedMatchNode(event.range, parent)
                    if (event.stage == values.lastIndex) return node.ranges()
                    windows[event.stage].add(node)
                }
                val value = values[event.stage]
                val next = text.indexOf(value, event.range.first + 1)
                if (next >= 0) events.add(OrderedOccurrence(event.stage, next until next + value.length))
            }
        }
        return null
    }

    private fun events(text: String): PriorityQueue<OrderedOccurrence>? {
        val queue = PriorityQueue(compareBy<OrderedOccurrence> { it.range.last }.thenBy { it.stage })
        values.forEachIndexed { index, value ->
            val offset = text.indexOf(value)
            if (offset < 0) return null
            queue.add(OrderedOccurrence(index, offset until offset + value.length))
        }
        return queue
    }
}

private data class OrderedOccurrence(
    val stage: Int,
    val range: IntRange,
)

private class OrderedMatchNode(
    val range: IntRange,
    val parent: OrderedMatchNode?,
) {
    fun ranges(): List<IntRange> = generateSequence(this) { it.parent }.map { it.range }.toList().asReversed()
}

/** A later reachable predecessor dominates an earlier one once both end before the next fragment. */
private class OrderedReachabilityWindow(
    private val nextLength: Int,
) {
    private val pending = ArrayDeque<OrderedMatchNode>()
    private var eligible: OrderedMatchNode? = null

    fun add(node: OrderedMatchNode) {
        advance(node.range.last - nextLength)
        pending.addLast(node)
    }

    fun parent(
        start: Int,
        gap: Int,
    ): OrderedMatchNode? {
        advance(start - 1)
        return eligible?.takeIf { start.toLong() - it.range.last - 1 <= gap }
    }

    private fun advance(end: Int) {
        while (pending.isNotEmpty() && pending.first().range.last <= end) eligible = pending.removeFirst()
    }
}
