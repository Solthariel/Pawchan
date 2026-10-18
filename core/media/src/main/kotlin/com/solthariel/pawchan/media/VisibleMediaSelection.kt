package com.solthariel.pawchan.media

import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.runtime.produceState
import com.solthariel.pawchan.core.ui.LocalPostDisplayPreferences
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Filtering a potentially complete thread/gallery never traverses its items on Main. */
@Composable
internal fun rememberVisibleMediaSelection(selection: MediaViewerSelection): MediaViewerSelection {
    if (!LocalPostDisplayPreferences.current.hideStickers) return selection
    return key(selection) {
        produceState(
            initialValue =
                selection.copy(
                    attachment = selection.attachment?.takeUnless { it.isSticker },
                    items = emptyList(),
                ),
        ) {
            value =
                withContext(Dispatchers.Default) {
                    selection.copy(
                        attachment = selection.attachment?.takeUnless { it.isSticker },
                        items = selection.items.filterNot { it.isSticker },
                    )
                }
        }.value
    }
}
