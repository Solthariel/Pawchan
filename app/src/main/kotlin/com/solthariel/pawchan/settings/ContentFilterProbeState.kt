package com.solthariel.pawchan.settings

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.text.AnnotatedString
import com.solthariel.pawchan.domain.filter.ContentFilterRule
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

internal data class FilterProbeInput(
    val rule: ContentFilterRule,
    val text: String,
    val threadId: Long? = rule.targets.threadIds.firstOrNull(),
)

/** Main-confined. Keep the last caption during recalculation; never apply stale highlighting. */
internal class ContentFilterProbeState(
    private val dispatcher: CoroutineDispatcher = Dispatchers.Default,
) {
    private var completed by mutableStateOf<Pair<FilterProbeInput, FilterProbeResult>?>(null)
    private var revision = 0L

    fun caption(text: String): String = if (text.isBlank()) "" else completed?.second?.caption.orEmpty()

    fun preview(input: FilterProbeInput): AnnotatedString? = completed?.takeIf { it.first == input }?.second?.preview

    suspend fun check(input: FilterProbeInput) {
        val request = ++revision
        if (input.text.isBlank()) {
            completed = null
        } else {
            val result = withContext(dispatcher) { filterProbeResult(input.rule, input.text, input.threadId) }
            if (request == revision) completed = input to result
        }
    }
}
