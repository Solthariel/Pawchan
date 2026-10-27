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
import com.solthariel.pawchan.domain.filter.contentFilterValidationError
import com.solthariel.pawchan.domain.model.BoardAddress
import com.solthariel.pawchan.domain.model.BoardId
import com.solthariel.pawchan.domain.model.SiteId

@Composable
internal fun ContentFilterProbe(
    rule: ContentFilterRule,
    samples: List<ContentFilterSample> = emptyList(),
    sourceText: String? = null,
    onTextChange: ((String) -> Unit)? = null,
) {
    var localText by rememberSaveable { mutableStateOf(initialFilterProbeText(rule, samples)) }
    val text = sourceText ?: localText
    val state = remember { ContentFilterProbeState() }
    val threadId = samples.firstOrNull()?.threadId ?: rule.targets.threadIds.firstOrNull()
    val input = FilterProbeInput(rule, text, threadId)
    LaunchedEffect(input) { state.check(input) }
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        FilterLineInput(
            "Проверяемый текст",
            text,
            { value -> if (onTextChange != null) onTextChange(value) else localText = value },
            presentation = ContentFilterInputPresentation(preview = state.preview(input)),
        )
        Text(
            state.caption(input),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            minLines = 1,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
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
    contentFilterValidationError(rule)?.let {
        return FilterProbeResult("Проверка недоступна: исправьте условие", AnnotatedString(text), false)
    }
    val site = SiteId.parse(rule.site)
    val board = BoardId.parse(rule.boards.firstOrNull() ?: "b")
    val ranges =
        if (site != null && board != null) {
            probeRanges(rule, BoardAddress(site, board), text, threadId)
        } else {
            emptyList()
        }
    val fragments = ranges.map { text.substring(it) }.distinct()
    val caption =
        if (fragments.isEmpty()) {
            "Пост останется видимым"
        } else {
            "Пост будет скрыт. Найдено: " + fragments.joinToString { "«$it»" }
        }
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
        engine.threadMatch(board, null, text, threadId),
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
