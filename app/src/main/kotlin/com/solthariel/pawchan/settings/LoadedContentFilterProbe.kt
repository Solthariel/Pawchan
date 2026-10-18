package com.solthariel.pawchan.settings

import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.text.AnnotatedString
import com.solthariel.pawchan.domain.filter.ContentFilterEngine
import com.solthariel.pawchan.domain.filter.ContentFilterRule
import com.solthariel.pawchan.domain.filter.ContentFilterSample
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
internal fun LoadedContentFilterProbe(
    rule: ContentFilterRule,
    samples: List<ContentFilterSample>,
) {
    if (samples.isEmpty()) return
    var requested by remember(rule, samples) { mutableStateOf(false) }
    var result by remember(rule, samples) { mutableStateOf<List<AnnotatedString>>(emptyList()) }
    LaunchedEffect(requested, rule, samples) {
        if (requested) result = withContext(Dispatchers.Default) { loadedMatches(rule, samples) }
    }
    TextButton(onClick = { requested = true }) { Text("Проверить на загруженных данных") }
    result.forEach { Text(it) }
}

private fun loadedMatches(
    rule: ContentFilterRule,
    samples: List<ContentFilterSample>,
): List<AnnotatedString> {
    val engines =
        samples.map { it.place }.distinct().associateWith { place ->
            ContentFilterEngine(listOf(rule.copy(enabled = true)), place)
        }
    val matches =
        samples.mapNotNull { sample ->
            val engine = engines.getValue(sample.place)
            val match =
                if (sample.comment) {
                    engine.commentMatch(sample.board, sample.text, sample.threadId)
                } else {
                    engine.threadMatch(sample.board, sample.subject, sample.text, sample.threadId)
                }
            match?.let { sample to it }
        }
    val threads = matches.count { !it.first.comment }
    val comments = matches.count { it.first.comment }
    return listOf(
        AnnotatedString(
            "В загруженных данных совпало тредов: $threads, комментариев: $comments. " +
                "Первый пост проверяется только в полученном от источника объёме. " +
                "Примеры (до $MAX_LOADED_FILTER_EXAMPLES):",
        ),
    ) +
        matches.take(MAX_LOADED_FILTER_EXAMPLES).map { (sample, match) ->
            highlightedMatches(listOf(match), sample.subject.orEmpty(), sample.text)
        }
}

private const val MAX_LOADED_FILTER_EXAMPLES = 10
