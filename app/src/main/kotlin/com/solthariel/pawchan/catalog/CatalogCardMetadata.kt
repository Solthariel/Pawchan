package com.solthariel.pawchan.catalog

import androidx.compose.runtime.Composable
import com.solthariel.pawchan.threads.ThreadListCardMetadata

@Composable
internal fun CatalogCardMetadata(thread: CatalogThreadUi) {
    ThreadListCardMetadata(thread.toThreadListCardUi())
}
