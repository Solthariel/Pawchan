package com.solthariel.pawchan.update

internal fun UpdateUiState.availableVersionName(): String? =
    when (this) {
        is UpdateUiState.Available -> release.versionName
        is UpdateUiState.Downloading -> release.versionName
        is UpdateUiState.Ready -> release.versionName
        else -> null
    }
