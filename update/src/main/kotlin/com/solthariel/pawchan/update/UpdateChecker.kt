package com.solthariel.pawchan.update

internal object UpdateChecker {
    fun isNewer(
        currentVersionCode: Long,
        release: UpdatePackage,
    ): Boolean = release.versionCode > currentVersionCode
}
