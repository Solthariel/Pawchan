package com.solthariel.pawchan.data.network

import com.solthariel.pawchan.domain.model.BoardAddress
import com.solthariel.pawchan.domain.posting.PostingFailure
import com.solthariel.pawchan.domain.posting.PostingFeatures
import com.solthariel.pawchan.domain.posting.PostingIdentityRules
import com.solthariel.pawchan.domain.posting.PostingOutcome
import com.solthariel.pawchan.domain.posting.PostingRules
import com.solthariel.pawchan.domain.posting.ReplyAttachment
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.intOrNull

internal fun decodePostingRules(
    board: BoardAddress,
    payload: String,
): PostingOutcome<PostingRules> {
    val root = Json.parseToJsonElement(payload) as? JsonObject
    val metadata = root?.get("board") as? JsonObject
    if ((metadata?.get("id") as? JsonPrimitive)?.contentOrNull != board.board.value) {
        return PostingOutcome.Failure(PostingFailure.InvalidResponse)
    }
    val defaults = PostingRules()
    val extensions =
        (metadata?.get("file_types") as? JsonArray)
            ?.mapNotNull {
                (it as? JsonPrimitive)?.contentOrNull?.lowercase()
            }?.toSet() ?: defaults.extensions
    val maxBytes = (metadata?.get("max_files_size") as? JsonPrimitive)?.intOrNull?.toLong()?.times(BYTES_PER_KIBIBYTE)
    // Dvach's regular post contract permits four files when the payload has no per-post count.
    val fileCount = (metadata?.get("max_files_count") as? JsonPrimitive)?.intOrNull ?: defaults.maxFiles
    val commentLimit = (metadata?.get("max_comment") as? JsonPrimitive)?.intOrNull ?: defaults.maxCommentLength
    return PostingOutcome.Success(
        PostingRules(
            maxFiles = if (extensions.isEmpty()) 0 else fileCount.coerceIn(0, PostingRules.MAX_LOCAL_FILES),
            maxFileBytes =
                maxBytes?.coerceIn(1, ReplyAttachment.MAX_ATTACHMENT_BYTES.toLong())?.toInt()
                    ?: defaults.maxFileBytes,
            maxCommentLength = commentLimit.coerceIn(1, defaults.maxCommentLength),
            identity =
                PostingIdentityRules(
                    namesEnabled = metadata?.flag("enable_names") ?: true,
                    icons = decodePostingIcons(metadata),
                ),
            features =
                PostingFeatures(
                    subject = metadata?.flag("enable_subject") ?: true,
                    sage = metadata?.flag("enable_sage") ?: true,
                    tags = metadata?.containsKey("tags") == true,
                    attachmentSpoilers = true,
                ),
            extensions = extensions + if ("jpg" in extensions) setOf("jpeg") else emptySet(),
        ),
    )
}

private const val BYTES_PER_KIBIBYTE = 1024
