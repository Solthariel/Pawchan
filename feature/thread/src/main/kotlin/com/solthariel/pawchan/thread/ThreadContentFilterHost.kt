package com.solthariel.pawchan.thread

import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import com.solthariel.pawchan.domain.filter.ContentFilterSample
import com.solthariel.pawchan.domain.model.BoardAddress

/** Text intent only. App owns the editor and persistence. */
data class ContentFilterSelection(
    val board: BoardAddress,
    val text: String,
    val comment: Boolean,
)

internal val LocalAddContentFilter = staticCompositionLocalOf<((ContentFilterSelection) -> Unit)?> { null }

data class ThreadContentFilterTools(
    val add: (ContentFilterSelection) -> Unit,
    val edit: (String) -> Unit,
    val editor: @Composable (List<ContentFilterSample>) -> Unit,
)

internal val LocalEditContentFilter = staticCompositionLocalOf<((String) -> Unit)?> { null }
