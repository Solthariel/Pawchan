package com.solthariel.pawchan.update

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class UpdateEntryChecksTest {
    @Test fun recompositionAndRowRecreationDoNotRepeatTheSameVisit() {
        val checks = UpdateEntryChecks()
        val visit = UpdateCheckRequest()
        assertTrue(checks.consume(visit, UpdateUiState.Idle))
        assertFalse(checks.consume(visit, UpdateUiState.Checking))
        assertFalse(checks.consume(visit, UpdateUiState.UpToDate))
        assertFalse(checks.consume(visit, UpdateUiState.Error(UpdateError.GENERAL)))
    }

    @Test fun leavingAndReturningCreatesANewCheckEvenAfterAPreviousFailure() {
        val checks = UpdateEntryChecks()
        assertTrue(checks.consume(UpdateCheckRequest(), UpdateUiState.Idle))
        assertFalse(checks.consume(null, UpdateUiState.UpToDate))
        assertTrue(checks.consume(UpdateCheckRequest(), UpdateUiState.Error(UpdateError.GENERAL)))
    }

    @Test fun noVisitMeansNoAutomaticNetworkCheck() {
        assertFalse(UpdateEntryChecks().consume(null, UpdateUiState.Idle))
    }

    @Test fun automaticCheckCannotDiscardADownloadOrAnApkWaitingToBeInstalled() {
        val checks = UpdateEntryChecks()
        val release =
            UpdatePackage().apply {
                versionName = "0.1.99"
                versionCode = 100
            }
        assertFalse(checks.consume(UpdateCheckRequest(), UpdateUiState.Downloading(release, 25)))
        assertFalse(checks.consume(UpdateCheckRequest(), UpdateUiState.Ready(release, "update.apk")))
    }
}
