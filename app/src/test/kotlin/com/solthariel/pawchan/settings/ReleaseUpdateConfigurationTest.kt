package com.solthariel.pawchan.settings

import com.solthariel.pawchan.BuildConfig
import org.junit.Assert.assertEquals
import org.junit.Test

class ReleaseUpdateConfigurationTest {
    @Test fun updatesUseTheConfiguredFeedWithoutPrivateAuthorization() {
        val configuration = releaseUpdateConfiguration()
        assertEquals(BuildConfig.UPDATE_MANIFEST_URL, configuration.manifestUrl)
        assertEquals(BuildConfig.VERSION_CODE.toLong(), configuration.currentVersionCode)
        assertEquals(BuildConfig.VERSION_NAME, configuration.currentVersionName)
    }
}
