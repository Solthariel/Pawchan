package com.solthariel.pawchan.update

import org.junit.Assert.assertEquals
import org.junit.Test

class UpdateInlineStateTest {
    @Test fun checkDownloadAndInstallAreDistinctStages() {
        val release = UpdatePackage()
        assertEquals(R.string.update_check, UpdateUiState.Idle.inlineLabel())
        assertEquals(R.string.update_checking, UpdateUiState.Checking.inlineLabel())
        assertEquals(R.string.update_check, UpdateUiState.UpToDate.inlineLabel())
        assertEquals(R.string.update_download, UpdateUiState.Available(release, null).inlineLabel())
        assertEquals(R.string.update_install, UpdateUiState.Ready(release, "verified.apk").inlineLabel())
        assertEquals(R.string.update_check, UpdateUiState.Error(UpdateError.GENERAL).inlineLabel())
    }
}
