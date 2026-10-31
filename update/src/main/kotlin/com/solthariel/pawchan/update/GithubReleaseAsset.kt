package com.solthariel.pawchan.update

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.longOrNull
import okhttp3.HttpUrl

internal suspend fun githubReleaseAsset(
    transport: UpdateTransport,
    urls: GithubUpdateUrls,
    source: HttpUrl,
    expectedLength: Long,
): HttpUrl {
    val (lookup, name) = urls.releaseLookup(source)
    val id = githubAssetId(transport.readText(lookup), name, expectedLength)
    return urls.asset(id)
}

internal fun githubAssetId(
    payload: String,
    name: String,
    expectedLength: Long,
): Long {
    val release = Json.parseToJsonElement(payload).jsonObject
    check(release["draft"]?.jsonPrimitive?.content == "false") { "Drafts are not installable updates" }
    val candidates =
        checkNotNull(release["assets"]).jsonArray.filter {
            it.jsonObject["name"]?.jsonPrimitive?.content == name
        }
    check(candidates.size == 1) { "Expected exactly one matching APK" }
    val asset = candidates.single().jsonObject
    check(asset["state"]?.jsonPrimitive?.content == "uploaded") { "Asset upload is incomplete" }
    check(asset["size"]?.jsonPrimitive?.longOrNull == expectedLength) { "Asset length differs from manifest" }
    return checkNotNull(asset["id"]?.jsonPrimitive?.longOrNull).also { check(it > 0) { "Invalid asset ID" } }
}
