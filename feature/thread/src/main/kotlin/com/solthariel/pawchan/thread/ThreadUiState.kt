package com.solthariel.pawchan.thread

import com.solthariel.pawchan.domain.model.DomainFailure
import com.solthariel.pawchan.domain.model.DomainOutcome
import com.solthariel.pawchan.domain.model.MediaAttachment
import com.solthariel.pawchan.domain.model.MediaDimensions
import com.solthariel.pawchan.domain.model.MediaKind
import com.solthariel.pawchan.domain.model.PostDocument
import com.solthariel.pawchan.domain.model.PostLink
import com.solthariel.pawchan.domain.model.PostRating
import com.solthariel.pawchan.domain.model.ThreadAddress
import com.solthariel.pawchan.domain.model.ThreadSnapshot
import com.solthariel.pawchan.domain.model.isRateLimited
import com.solthariel.pawchan.media.MEDIA_ATTACHMENT_COLUMNS
import com.solthariel.pawchan.media.MediaAttachmentUi
import com.solthariel.pawchan.media.toMediaAttachmentUi
import com.solthariel.pawchan.media.withMediaOwner

internal sealed interface ThreadUiState {
    data object Loading : ThreadUiState

    data class Content(
        val id: Long,
        val subject: String?,
        val posts: List<ThreadPostUi>,
        val flags: ThreadContentFlags,
        val presentation: ThreadContentPresentation,
        val statistics: ThreadContentStatistics,
        val address: ThreadAddress? = null,
    ) : ThreadUiState {
        val closed: Boolean
            get() = flags.closed
        val pinned: Boolean
            get() = flags.pinned
        val endless: Boolean
            get() = flags.endless
        val isFavorite: Boolean
            get() = presentation.isFavorite
        val isRefreshing: Boolean
            get() = presentation.isRefreshing
        val replies: Int
            get() = statistics.replies
        val files: Int
            get() = statistics.files
        val mediaBytes: Long
            get() = statistics.mediaBytes
        val mediaBytesComplete: Boolean
            get() = statistics.mediaBytesComplete
    }

    data class Error(
        val reason: ThreadError,
        val challengeUrl: String? = null,
    ) : ThreadUiState
}

internal data class ThreadContentFlags(
    val closed: Boolean,
    val pinned: Boolean = false,
    val endless: Boolean = false,
)

internal data class ThreadContentStatistics(
    val replies: Int = 0,
    val files: Int = 0,
    val postsWithFiles: Int = 0,
    val mediaBytes: Long = 0L,
    val mediaBytesComplete: Boolean = true,
    val linkAttachments: Int = 0,
    val uniquePosters: Int = 0,
)

internal data class ThreadPostUi(
    val id: Long,
    val document: PostDocument,
    val richText: RichPostTextUi,
    val metadata: ThreadPostMetadataUi,
    val attachments: List<MediaAttachmentUi>,
    val attachmentRows: List<List<MediaAttachmentUi>> = attachments.chunked(MEDIA_ATTACHMENT_COLUMNS),
) {
    val mediaDisplay =
        com.solthariel.pawchan.media
            .PostMediaDisplay(attachments, attachmentRows)

    val author: String?
        get() = metadata.author
    val date: String?
        get() = metadata.date
    val sage: Boolean
        get() = metadata.sage
    val referencedPostIds: Set<Long>
        get() =
            document.runs
                .mapNotNull { run -> (run.link as? PostLink.Reference)?.post?.value }
                .toSet()
}

internal data class ThreadPostMetadataUi(
    val author: String?,
    val date: String?,
    val sage: Boolean,
    val display: ThreadPostDisplay = ThreadPostDisplay(),
    val changes: ThreadPostChanges = ThreadPostChanges(),
    val visibility: ThreadPostVisibility = ThreadPostVisibility(),
    val rating: PostRating? = null,
) {
    val hidden: Boolean
        get() = visibility.hidden
    val contentFilterLabel: String?
        get() = visibility.contentFilterLabel
    val contentFilterId: String?
        get() = visibility.contentFilterId
}

internal enum class ThreadError {
    NETWORK,
    TIMEOUT,
    SECURE_CONNECTION,
    NOT_FOUND,
    INVALID_RESPONSE,
    SERVER,
    RATE_LIMITED,
    PROTECTION,
    STORAGE,
}

internal fun DomainOutcome<ThreadSnapshot>.toThreadUiState(isFavorite: Boolean = false): ThreadUiState =
    when (this) {
        is DomainOutcome.Success -> {
            ThreadUiState
                .Content(
                    id = value.address.thread.value,
                    subject = value.subject,
                    posts =
                        value.posts.zip(value.liveOrdinals()) { post, index ->
                            ThreadPostUi(
                                id = post.address.post.value,
                                document = post.content.document,
                                richText = RichPostTextUi.create(post.content.document),
                                metadata =
                                    ThreadPostMetadataUi(
                                        author = post.content.metadata.name,
                                        date = post.content.metadata.date,
                                        sage = post.content.metadata.sage,
                                        display =
                                            preparePostDisplay(
                                                post,
                                                index,
                                                ThreadOpeningInfo(value.flags, value.tag),
                                            ),
                                        changes = ThreadPostChanges(deleted = post.address.post in value.deletedPosts),
                                        rating = post.content.rating,
                                    ),
                                attachments =
                                    post.attachments
                                        .map(
                                            MediaAttachment::toMediaAttachmentUi,
                                        ).withMediaOwner(
                                            post.address.post.value,
                                            postDownloadPrefix(post.address),
                                        ),
                            )
                        },
                    flags =
                        ThreadContentFlags(
                            closed = value.flags.closed,
                            pinned = value.flags.pinned,
                            endless = value.flags.endless,
                        ),
                    presentation = ThreadContentPresentation(isFavorite),
                    statistics = value.toThreadContentStatistics(),
                    address = value.address,
                ).withPostNavigation()
        }

        is DomainOutcome.Failure -> {
            reason.toThreadErrorState()
        }
    }

private fun ThreadSnapshot.toThreadContentStatistics(): ThreadContentStatistics =
    ThreadContentStatistics(
        replies = posts.count { post -> post.address.post.value != address.thread.value },
        files = posts.sumOf { post -> post.attachments.size },
        postsWithFiles = posts.count { post -> post.attachments.isNotEmpty() },
        mediaBytes =
            posts.sumOf { post ->
                post.attachments.sumOf { attachment -> attachment.byteSize?.value ?: 0L }
            },
        mediaBytesComplete =
            posts.all { post ->
                post.attachments.all { attachment -> attachment.byteSize != null }
            },
        linkAttachments =
            posts.sumOf { post ->
                post.content.document.runs
                    .count { run -> run.link is PostLink.External }
            },
        uniquePosters = uniquePosters,
    )

internal fun DomainFailure.toThreadError(): ThreadError =
    when (this) {
        DomainFailure.NetworkUnavailable -> {
            ThreadError.NETWORK
        }

        DomainFailure.NetworkTimeout -> {
            ThreadError.TIMEOUT
        }

        DomainFailure.SecureConnectionFailed -> {
            ThreadError.SECURE_CONNECTION
        }

        DomainFailure.NotFound -> {
            ThreadError.NOT_FOUND
        }

        DomainFailure.StorageUnavailable -> {
            ThreadError.STORAGE
        }

        DomainFailure.InvalidResponse -> {
            ThreadError.INVALID_RESPONSE
        }

        is DomainFailure.HttpStatus -> {
            if (isRateLimited) ThreadError.RATE_LIMITED else ThreadError.SERVER
        }

        is DomainFailure.ProtectionChallenge -> {
            ThreadError.PROTECTION
        }
    }

private fun DomainFailure.toThreadErrorState(): ThreadUiState.Error =
    ThreadUiState.Error(
        reason = toThreadError(),
        challengeUrl = (this as? DomainFailure.ProtectionChallenge)?.verificationUrl?.value,
    )

/** Порядковые номера как в Dashchan: живые посты нумеруются подряд, удалённые получают 0 и показываются как «X». */
private fun ThreadSnapshot.liveOrdinals(): List<Int> {
    var next = 0
    return posts.map { post -> if (post.address.post in deletedPosts) 0 else ++next }
}
