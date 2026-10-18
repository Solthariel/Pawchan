package com.solthariel.pawchan.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.solthariel.pawchan.domain.filter.ContentFilterEngine
import com.solthariel.pawchan.domain.filter.ContentFilterRule
import com.solthariel.pawchan.domain.filter.ContentFilterSample
import com.solthariel.pawchan.domain.filter.FilterMatchField
import com.solthariel.pawchan.domain.model.BoardAddress
import com.solthariel.pawchan.domain.model.BoardId
import com.solthariel.pawchan.domain.model.SiteId
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
internal fun ContentFilterProbe(
    rule: ContentFilterRule,
    samples: List<ContentFilterSample> = emptyList(),
) {
    var text by rememberSaveable { mutableStateOf("") }
    var result by remember(rule, text) { mutableStateOf<FilterProbeResult?>(null) }
    LaunchedEffect(rule, text) {
        if (text.isNotBlank()) {
            val threadId = samples.firstOrNull()?.threadId ?: rule.targets.threadIds.firstOrNull()
            result = withContext(Dispatchers.Default) { filterProbeResult(rule, text, threadId) }
        }
    }
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        FilterLineInput(
            "Проверяемый текст",
            text,
            { text = it },
            presentation = ContentFilterInputPresentation(preview = result?.preview),
        )
        result?.let { checked ->
            Text(
                checked.caption,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

internal data class FilterProbeResult(
    val caption: String,
    val preview: AnnotatedString,
    val matched: Boolean,
)

internal fun filterProbeResult(
    rule: ContentFilterRule,
    text: String,
    threadId: Long? = rule.targets.threadIds.firstOrNull(),
): FilterProbeResult {
    val site = SiteId.parse(rule.site)
    val board = BoardId.parse(rule.boards.firstOrNull() ?: "b")
    val ranges =
        if (site != null && board != null) {
            probeRanges(rule, BoardAddress(site, board), text, threadId)
        } else {
            emptyList()
        }
    val fragments = ranges.map { text.substring(it) }.distinct()
    val caption = if (fragments.isEmpty()) "Нет совпадений" else "Совпало: " + fragments.joinToString { "«$it»" }
    val preview =
        buildAnnotatedString {
            append(text)
            ranges.forEach { range ->
                addStyle(SpanStyle(background = Color(FILTER_PROBE_HIGHLIGHT_ARGB)), range.first, range.last + 1)
            }
        }
    return FilterProbeResult(caption, preview, fragments.isNotEmpty())
}

private fun probeRanges(
    rule: ContentFilterRule,
    board: BoardAddress,
    text: String,
    threadId: Long?,
): List<IntRange> {
    val engine = ContentFilterEngine(listOf(rule.copy(enabled = true)))
    return listOfNotNull(
        engine.threadMatch(board, text, text, threadId),
        engine.commentMatch(board, text, threadId),
    ).flatMap { it.ranges }.distinct()
}

internal fun highlightedMatches(
    matches: List<com.solthariel.pawchan.domain.filter.FilterMatch>,
    subject: String,
    text: String,
): AnnotatedString =
    buildAnnotatedString {
        matches.forEach { match ->
            val source = if (match.field == FilterMatchField.SUBJECT) subject else text
            append("Совпало: ")
            append(match.ranges.joinToString { "«${source.substring(it)}»" })
            append("\n")
        }
    }

private const val FILTER_PROBE_HIGHLIGHT_ARGB = 0x66FFD54F
