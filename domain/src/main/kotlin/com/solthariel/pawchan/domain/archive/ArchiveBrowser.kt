package com.solthariel.pawchan.domain.archive

import com.solthariel.pawchan.domain.model.DomainOutcome
import com.solthariel.pawchan.domain.model.MediaDimensions
import com.solthariel.pawchan.domain.model.PostDocument

/** An archive is not a board: its thread IDs and tag IDs belong to the archive itself. */
data class ArchiveTag(
    val id: Long,
    val title: String,
)

data class ArchiveThreadMetadata(
    val previewText: String = "",
    val status: String? = null,
    val date: String? = null,
    val updatedAt: String? = null,
    val lastPostAt: String? = null,
    val favorite: Boolean = false,
)

data class ArchiveThreadSummary(
    val id: Long,
    val title: String,
    val postCount: Int,
    val thumbnailUrl: String?,
    val tags: List<ArchiveTag>,
    val media: List<ArchiveMedia> = emptyList(),
    val metadata: ArchiveThreadMetadata = ArchiveThreadMetadata(),
)

data class ArchiveThreadPage(
    val threads: List<ArchiveThreadSummary>,
    val hasNext: Boolean,
    val totalPages: Int? = null,
)

data class ArchiveMedia(
    val url: String,
    val thumbnailUrl: String?,
    val details: ArchiveMediaDetails? = null,
)

data class ArchiveMediaDetails(
    val sizeLabel: String?,
    val dimensions: MediaDimensions?,
)

data class ArchivePost(
    val number: String?,
    val author: String?,
    val date: String?,
    val text: String,
    val media: List<ArchiveMedia>,
    val details: ArchivePostDetails = ArchivePostDetails(),
) {
    val subject: String? get() = details.subject
    val document: PostDocument? get() = details.document
}

data class ArchivePostDetails(
    val subject: String? = null,
    val document: PostDocument? = null,
    val timestamp: Long? = null,
    val originalPoster: Boolean = false,
    val sage: Boolean = false,
)

data class ArchiveThread(
    val id: Long,
    val title: String,
    val posts: List<ArchivePost>,
)

interface ArchiveBrowser {
    suspend fun searchTags(query: String): DomainOutcome<List<ArchiveTag>>

    suspend fun loadThreads(
        tags: List<Long>,
        page: Int,
    ): DomainOutcome<ArchiveThreadPage>

    suspend fun loadThread(id: Long): DomainOutcome<ArchiveThread>
}
