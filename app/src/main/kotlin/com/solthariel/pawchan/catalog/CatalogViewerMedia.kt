package com.solthariel.pawchan.catalog

import com.solthariel.pawchan.media.viewerItems

internal fun CatalogUiState.withViewerMedia(): CatalogUiState =
    if (this is CatalogUiState.Content) {
        copy(viewerMediaGroups = threads.filterNot { it.hidden }.associate { it.id to it.attachments.viewerItems() })
    } else {
        this
    }
