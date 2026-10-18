package com.solthariel.pawchan.update

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
internal data class UpdateManifest(
    val applications: List<UpdateApplication>,
)

@Serializable
internal data class UpdateApplication(
    val name: String,
    val packages: List<UpdatePackage>,
)

@Serializable
internal class UpdatePackage {
    @SerialName("version_name")
    lateinit var versionName: String

    @SerialName("version_code")
    var versionCode: Long = 0

    @SerialName("min_sdk")
    var minSdk: Int = 0

    var length: Long = 0
    var source: String? = null
    var changelog: String? = null

    @SerialName("sha256sum")
    var sha256: String? = null
}

public data class UpdateConfiguration(
    val manifestUrl: String,
    val currentVersionCode: Long,
    val currentVersionName: String,
)

internal sealed interface UpdateUiState {
    data object Idle : UpdateUiState

    data object Checking : UpdateUiState

    data object UpToDate : UpdateUiState

    data class Available(
        val release: UpdatePackage,
        val changelog: String?,
    ) : UpdateUiState

    data class Downloading(
        val release: UpdatePackage,
        val progress: Int,
    ) : UpdateUiState

    data class Ready(
        val release: UpdatePackage,
        val apkPath: String,
        val permissionRequired: Boolean = false,
    ) : UpdateUiState

    data class Error(
        val reason: UpdateError,
    ) : UpdateUiState
}

internal enum class UpdateError {
    NOT_CONFIGURED,
    GENERAL,
}
