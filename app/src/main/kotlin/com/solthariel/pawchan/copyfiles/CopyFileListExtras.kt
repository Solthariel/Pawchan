package com.solthariel.pawchan.copyfiles

internal data class CopyFileListExtras(
    val additional: List<CopyDocument>,
    val unavailable: List<CopyDocument>,
    val onMore: (CopyDocument) -> Unit,
)
