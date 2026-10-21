package com.solthariel.pawchan.domain.posting

/** Service policy is separate from the size of the media selection grid. */
data class PostingRules(
    val maxFiles: Int = 4,
    val maxFileBytes: Int = ReplyAttachment.MAX_ATTACHMENT_BYTES,
    val maxCommentLength: Int = ReplyDraft.MAX_COMMENT_LENGTH,
    val identity: PostingIdentityRules = PostingIdentityRules(),
    val features: PostingFeatures = PostingFeatures(),
    val maxTotalFileBytes: Int = DEFAULT_MAX_TOTAL_FILE_BYTES,
    val extensions: Set<String> = setOf("jpg", "jpeg", "png", "gif", "webp", "mp4", "webm"),
) {
    val namesEnabled: Boolean get() = identity.namesEnabled
    val icons: List<PostingIcon> get() = identity.icons
    val subjectEnabled: Boolean get() = features.subject
    val sageEnabled: Boolean get() = features.sage
    val tagsEnabled: Boolean get() = features.tags

    init {
        require(maxFiles in 0..MAX_LOCAL_FILES)
        require(maxFileBytes > 0)
        require(maxTotalFileBytes > 0)
        require(maxCommentLength > 0)
    }

    fun accepts(files: List<ReplyAttachment>): Boolean =
        files.size <= maxFiles && acceptsSelection(files) &&
            files.sumOf { it.content.size.toLong() } <= maxTotalFileBytes

    /** Local selection capacity is not the account's posting permission. */
    fun acceptsSelection(files: List<ReplyAttachment>): Boolean =
        files.size <= MAX_LOCAL_FILES &&
            files.sumOf { it.content.size.toLong() } <= MAX_TOTAL_BYTES &&
            files.all { file ->
                file.content.size <= maxFileBytes &&
                    file.fileName.substringAfterLast('.', "").lowercase() in extensions
            }

    companion object {
        const val DEFAULT_MAX_TOTAL_FILE_BYTES: Int = 40 * 1024 * 1024
        const val MAX_LOCAL_FILES: Int = 8
        const val MAX_TOTAL_BYTES: Long = 64L * 1024 * 1024
    }
}

data class PostingFeatures(
    val subject: Boolean = true,
    val sage: Boolean = true,
    val tags: Boolean = false,
    val attachmentSpoilers: Boolean = false,
)
