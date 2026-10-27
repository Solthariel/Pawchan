package com.solthariel.pawchan.settings

internal data class ContentFilterMenuActions(
    val add: () -> Unit,
    val backup: ContentFilterBackupActions,
    val statistics: () -> Unit,
    val help: () -> Unit = {},
)
