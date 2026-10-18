package com.solthariel.pawchan.data.network

import com.solthariel.pawchan.domain.posting.AccountUploadLimits
import com.solthariel.pawchan.domain.posting.PostingRules
import com.solthariel.pawchan.domain.posting.ReplyAttachment
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.longOrNull

/** Only used for a result=2 response, not an anonymous or disabled captcha. */
internal fun decodeAccountUploadLimits(root: JsonObject): AccountUploadLimits? {
    val files = (root["maxFiles"] as? JsonPrimitive)?.intOrNull ?: 0
    val kibibytes = (root["maxFilesSize"] as? JsonPrimitive)?.longOrNull ?: 0
    if (files <= 0 || kibibytes <= 0) return null
    val boundedKiB = kibibytes.coerceAtMost(ReplyAttachment.MAX_ATTACHMENT_BYTES.toLong() / BYTES_PER_KIBIBYTE)
    return AccountUploadLimits(
        files.coerceAtMost(PostingRules.MAX_LOCAL_FILES),
        (boundedKiB * BYTES_PER_KIBIBYTE).toInt(),
    )
}

private const val BYTES_PER_KIBIBYTE = 1024
