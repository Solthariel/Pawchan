package com.solthariel.pawchan.boards

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.solthariel.pawchan.core.ui.UiChoiceSegments
import com.solthariel.pawchan.domain.model.SiteId
import com.solthariel.pawchan.infrastructure.sites.ForumBoardSource

@Composable
internal fun BoardSourcePicker(
    sources: List<ForumBoardSource>,
    selected: SiteId,
    modifier: Modifier = Modifier,
    archiveSelected: Boolean = false,
    onSelect: (SiteId) -> Unit,
) {
    if (sources.isEmpty()) return
    Box(
        modifier.fillMaxWidth().heightIn(min = SOURCE_PICKER_HEIGHT).testTag("board-source-panel"),
        contentAlignment = Alignment.Center,
    ) {
        UiChoiceSegments(
            options = sources.map { it.title },
            selected = if (archiveSelected) -1 else sources.indexOfFirst { it.site == selected },
            enabled = true,
            wrapLabels = true,
        ) { index -> onSelect(sources[index].site) }
    }
}

internal val SOURCE_PICKER_HEIGHT = 56.dp
