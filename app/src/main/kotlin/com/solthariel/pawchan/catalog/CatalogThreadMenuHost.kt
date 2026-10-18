package com.solthariel.pawchan.catalog

import com.solthariel.pawchan.domain.model.ThreadAddress
import kotlinx.coroutines.flow.StateFlow

/** App owns storage, registered site adapters, clipboard and external sharing. */
internal data class CatalogThreadMenuHost(
    val hiddenThreads: StateFlow<Set<ThreadAddress>>,
    val setHidden: (ThreadAddress, Boolean) -> Unit,
    val threadLink: (ThreadAddress) -> String?,
    val copyLink: (String) -> Unit,
    val shareLink: (String) -> Unit,
    val isFavorite: suspend (ThreadAddress) -> Boolean = { false },
    val setFavorite: (ThreadAddress, String?, Boolean) -> Unit = { _, _, _ -> },
)
