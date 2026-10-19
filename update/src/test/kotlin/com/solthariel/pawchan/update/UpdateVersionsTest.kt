package com.solthariel.pawchan.update

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class UpdateVersionsTest {
    @Test fun availableVersionRemainsVisibleWhileDownloadingAndReadyToInstall() {
        val release = UpdatePackage().apply { versionName = "0.1.5" }
        val states =
            listOf(
                UpdateUiState.Available(release, "* Запись"),
                UpdateUiState.Downloading(release, 50),
                UpdateUiState.Ready(release, "/cache/update.apk"),
            )
        states.forEach { assertEquals("0.1.5", it.availableVersionName()) }
    }

    @Test fun statusesWithoutAnOfferedPackageDoNotInventAVersion() {
        val states =
            listOf(
                UpdateUiState.Idle,
                UpdateUiState.Checking,
                UpdateUiState.UpToDate,
                UpdateUiState.Error(UpdateError.GENERAL),
            )
        states.forEach { assertNull(it.availableVersionName()) }
    }
}
