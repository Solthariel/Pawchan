package com.solthariel.pawchan.update

import kotlinx.serialization.json.Json

internal object UpdateManifestDecoder {
    private val json = Json { ignoreUnknownKeys = true }

    fun decodeClientPackage(payload: String): UpdatePackage {
        val manifest = json.decodeFromString<UpdateManifest>(payload)
        val client = manifest.applications.firstOrNull { it.name == CLIENT_NAME }
        return checkNotNull(client?.packages?.maxByOrNull(UpdatePackage::versionCode)) {
            "Update manifest has no client package"
        }
    }

    private const val CLIENT_NAME = "client"
}
