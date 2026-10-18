package com.solthariel.pawchan.core.ui

data class RefreshFailure(
    val rateLimited: Boolean = false,
    val challengeUrl: String? = null,
)
