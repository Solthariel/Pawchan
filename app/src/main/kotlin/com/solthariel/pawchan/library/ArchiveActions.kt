package com.solthariel.pawchan.library

import com.solthariel.pawchan.domain.model.ThreadAddress

internal data class ArchiveActions(
    val onBack: () -> Unit,
    val onRefresh: () -> Unit,
    val onPrevious: () -> Unit,
    val onNext: () -> Unit,
    val onThread: (ThreadAddress) -> Unit,
)
