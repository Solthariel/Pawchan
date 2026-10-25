package com.solthariel.pawchan.composer

import android.os.Trace
import androidx.compose.foundation.ScrollState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.layout.layout
import kotlinx.coroutines.flow.collect

/** Benchmark-only observer. Disabled for the original measurement protocol. */
@Composable
internal fun ObserveDiagnosticScroll(
    enabled: Boolean,
    scroll: ScrollState,
) {
    if (!enabled) return
    LaunchedEffect(scroll) {
        var scrolling = false
        try {
            snapshotFlow { scroll.value to scroll.isScrollInProgress }.collect { (value, active) ->
                Trace.setCounter("paw.scrollY", value.toLong())
                if (active != scrolling) {
                    if (active) Trace.beginAsyncSection("paw.scroll", 0) else Trace.endAsyncSection("paw.scroll", 0)
                    scrolling = active
                }
            }
        } finally {
            if (scrolling) Trace.endAsyncSection("paw.scroll", 0)
        }
    }
}

internal fun Modifier.diagnosticLayout(
    enabled: Boolean,
    expanded: Boolean,
    research: Boolean = false,
): Modifier {
    if (!enabled) return this
    val state = if (expanded) "long" else "short"
    return this
        .layout { measurable, constraints ->
            val child =
                diagnosticTrace("paw.measure.$state") {
                    if (research && expanded) {
                        AlignmentResearch.measure { measurable.measure(constraints) }
                    } else {
                        measurable.measure(constraints)
                    }
                }
            layout(child.width, child.height) {
                diagnosticTrace("paw.place.$state") {
                    if (research && expanded) AlignmentResearch.placed()
                    child.placeRelative(0, 0)
                }
            }
        }.drawWithContent {
            diagnosticTrace("paw.draw.$state") { drawContent() }
        }
}

internal inline fun <T> diagnosticTrace(
    label: String,
    block: () -> T,
): T {
    Trace.beginSection(label)
    return try {
        block()
    } finally {
        Trace.endSection()
    }
}
