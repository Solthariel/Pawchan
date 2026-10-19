package com.solthariel.pawchan.domain.statistics

private val POST_COUNT_COMMAND = Regex("(?<!\\S)/postcount(?!\\S)")

fun hasPostCountCommand(comment: String): Boolean = POST_COUNT_COMMAND.containsMatchIn(comment)

/** Keeps the typed post intact; the current submission is not counted before server acceptance. */
fun appendPostCount(
    comment: String,
    days: List<UsageDay>,
): String {
    val totals = usageTotals(days)
    return comment.trimEnd() + "\n\nСтатистика за всё время:\n" +
        UsageMetric.entries.joinToString("\n") { metric ->
            val unit = if (metric == UsageMetric.BYTES_DOWNLOADED) " байт" else ""
            "${metric.usageLabel()}: ${totals.getValue(metric)}$unit"
        }
}
