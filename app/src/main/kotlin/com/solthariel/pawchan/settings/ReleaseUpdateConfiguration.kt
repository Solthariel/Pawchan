package com.solthariel.pawchan.settings

import com.solthariel.pawchan.BuildConfig
import com.solthariel.pawchan.update.UpdateConfiguration

/** Anonymous updates only. A private feed is expected to fail without prompting for credentials. */
internal fun releaseUpdateConfiguration(): UpdateConfiguration =
    UpdateConfiguration(
        manifestUrl = BuildConfig.UPDATE_MANIFEST_URL,
        currentVersionCode = BuildConfig.VERSION_CODE.toLong(),
        currentVersionName = BuildConfig.VERSION_NAME,
    )
